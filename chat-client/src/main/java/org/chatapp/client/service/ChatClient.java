package org.chatapp.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chatapp.client.interfaces.ChatClientListener;
import org.chatapp.client.service.security.ClientTls;
import org.chatapp.client.ui.ChatClientGui;
import org.chatapp.common.model.*;
import org.chatapp.common.model.socket.transport.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatClient {
    // Socket and input/output streams
    private SSLSocket socket = null;
    private DataOutputStream output = null;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Instant lastMessageSent;
    private final Thread outputHandlerThread;
    private ServerOutputHandler outputHandler;

    private UserDTO sender;

    // ChatClientListener is used to send events from the output handler to chat client gui
    private ChatClientListener listener;

    private final ObjectMapper objectMapper;

    // Constructor to put IP address and port
    public ChatClient(String ip, int port, ChatClientListener listener, IncomingDataPersistence incomingDataPersistence){

        this.objectMapper = new ObjectMapper();
        this.listener = listener;
        createConnection(ip, port);
        schedulePingCheck();

        // Initializing and starting the thread used for output handling
        outputHandlerThread = createHandlerThread(incomingDataPersistence);
    }

    // Method that initializes a ScheduledExecutorService to run the pingIfNoActivity method
    private void schedulePingCheck() {
        scheduler.scheduleAtFixedRate(this::pingIfNoActivity, 30, 30, TimeUnit.SECONDS);
    }

    // Pings the server if there hasn't been a message in 30 seconds
    private void pingIfNoActivity() {
        if(Duration.between(lastMessageSent, Instant.now()).getSeconds() > 30) {
            sendPing();
        }
    }

    // Message that sends ping to socket server
    private void sendPing() {
        Instant currentInstant = Instant.now();
        SocketEvent<String> pingEvent = SocketEvent.<String>builder()
                .type(SocketEventType.PING_PONG)
                .payload(currentInstant.toString())
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(pingEvent));
            lastMessageSent = currentInstant;
            System.out.println("SENT PING AT " + lastMessageSent);
        } catch (IOException e) {
            System.out.println("Error sending ping to socket server: " + e.getMessage());
        }
    }

    // Sends initial socket message with the auth token
    public boolean sendAuthMessage(String token) {
        try {
            output.writeUTF("Bearer " + token);
            lastMessageSent = Instant.now();
        } catch (IOException e) {
            System.out.println("Error sending authentication message for socket connection: " + e.getMessage());
            return false;
        }

        return true;
    }

    // This method notifies the socket server that he user has logged out, and that the server is free to close the socket
    public boolean sendLogoutMessage(){
        SocketEvent<UserStatusChangeDTO> logoutEvent = SocketEvent.<UserStatusChangeDTO>builder()
                .type(SocketEventType.CHATTER_STATUS_CHANGE)
                .payload(UserStatusChangeDTO.builder()
                        .status(UserStatus.LOGOUT)
                        .build())
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(logoutEvent));
            lastMessageSent = Instant.now();
        } catch (IOException e) {
            System.out.println("Error sending log-out message for socket connection: " + e.getMessage());
            return false;
        }

        return true;
    }

    // Sends the ids of all the users with which the logged-in user has chats(group or private)
    public boolean sendPresenceSnapRequest(Set<UUID> userIds) {
        SocketEvent<Set<UUID>> snapshotRequest = SocketEvent.<Set<UUID>>builder()
                .type(SocketEventType.PRESENCE_SNAPSHOT)
                .payload(userIds)
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(snapshotRequest));
            lastMessageSent = Instant.now();
        } catch (IOException e) {
            System.out.println("Error sending presence snapshot request to the socket server: " + e.getMessage());
            return false;
        }
        return true;
    }

    // Send a message through the socket
    public boolean sendMessage(String messageContent, ChatDTO chatDTO, MessageType messageType, UUID replyId) {
        // Creating a message object and sending it to server through the socket
        MessageDTO message = MessageDTO.builder()
                .sender(sender)
                .chatId(chatDTO.getId())
                .content(messageContent)
                .sentTime(Instant.now())
                .messageType(messageType)
                .replyToId(replyId)
                .build();

        SocketEvent<MessageDTO> messageEvent = SocketEvent.<MessageDTO>builder()
                .type(SocketEventType.CHAT_MESSAGE)
                .payload(message)
                .build();

        try {
            output.writeUTF(objectMapper.writeValueAsString(messageEvent));
            lastMessageSent = Instant.now();
        } catch (IOException e) {
            System.out.println("Error sending chat message to the socket server: \n" + Arrays.toString(e.getStackTrace()));
            return false;
        }
        return true;
    }

    // Send the delete event for message through the socket
    public boolean sendDeleteMessage(MessageDTO message) {
        // Creating the socket event object and sending it to server through socket
        SocketEvent<MessageDTO> messageEvent = SocketEvent.<MessageDTO>builder()
                .type(SocketEventType.DELETE_MESSAGE)
                .payload(message)
                .build();

        try {
            output.writeUTF(objectMapper.writeValueAsString(messageEvent));
            lastMessageSent = Instant.now();
        } catch (IOException e) {
            System.out.println("Error sending delete message to the socket server: \n" + Arrays.toString(e.getStackTrace()));
            return false;
        }
        return true;
    }

    // Sends the first message of a regular chat through the socket
    public boolean sendFirstMessage(String messageContent, ChatDTO chatDTO, MessageType messageType) {
        // Creating a message object and sending it to server through the socket
        MessageDTO message = MessageDTO.builder()
                .sender(sender)
                .chatId(chatDTO.getId())
                .content(messageContent)
                .sentTime(Instant.now())
                .messageType(messageType)
                .build();

        // Creating the first message dto to be sent as socket event payload
        FirstMessageDTO firstMessageDTO = FirstMessageDTO.builder()
                .messageDTO(message)
                .chatDTO(chatDTO)
                .build();

        // Building the socket event dto
        SocketEvent<FirstMessageDTO> messageEvent = SocketEvent.<FirstMessageDTO>builder()
                .type(SocketEventType.FIRST_MESSAGE)
                .payload(firstMessageDTO)
                .build();

        try {
            output.writeUTF(objectMapper.writeValueAsString(messageEvent));
            lastMessageSent = Instant.now();
        } catch (IOException e) {
            System.out.println("Error sending chat message to the socket server: \n" + Arrays.toString(e.getStackTrace()));
            return false;
        }

        return true;
    }

    // Sends a create group chat event through the socket
    public boolean sendCreateGroup(ChatDTO groupChat){
        SocketEvent<ChatDTO> createdEvent = SocketEvent.<ChatDTO>builder()
                .type(SocketEventType.CREATE_GROUP)
                .payload(groupChat)
                .build();
        try{
            output.writeUTF(objectMapper.writeValueAsString(createdEvent));
            lastMessageSent = Instant.now();
        } catch (IOException e) {
            System.out.println("Error sending create group chat event to the socket server: " + e.getMessage());
            return false;
        }
        return true;
    }

    // Sends a search query through the socket
    public boolean sendSearchQuery(String query){
        SocketEvent<SearchRequestDTO> searchQuery = SocketEvent.<SearchRequestDTO>builder()
                .type(SocketEventType.SEARCH)
                .payload(SearchRequestDTO.builder()
                        .senderId(sender.getId())
                        .searchQuery(query)
                        .build())
                .build();

        try {
            output.writeUTF(objectMapper.writeValueAsString(searchQuery));
            lastMessageSent = Instant.now();
        } catch (IOException e) {
            System.out.println("Error sending search query: " + e.getMessage());
            return false;
        }

        return true;
    }

    // Method used to send message status update for a message
    public boolean sendStatusUpdateMessage(MessageDTO statusMessage, SocketEventType statusType) {
        SocketEvent<MessageDTO> statusUpdate = SocketEvent.<MessageDTO>builder()
                .type(statusType)
                .payload(statusMessage)
                .build();
        try{
            output.writeUTF(objectMapper.writeValueAsString(statusUpdate));
            lastMessageSent = Instant.now();
            return true;
        } catch (IOException e) {
            System.out.println("Error while sending message status update, error message: " + e.getMessage());
            return false;
        }
    }

    // Method used to send bulk seen message status update
    public boolean sendBulkSeenStatusUpdate(UserDTO sender, UUID chatId){
        SocketEvent<BulkMessageStatusDTO> seenUpdateEvent = SocketEvent.<BulkMessageStatusDTO>builder()
                .type(SocketEventType.BULK_STATUS_UPDATE)
                .payload(BulkMessageStatusDTO.builder()
                        .senderId(sender.getId())
                        .chatId(chatId)
                        .build())
                .build();
        try{
            output.writeUTF(objectMapper.writeValueAsString(seenUpdateEvent));
            lastMessageSent = Instant.now();
            return true;
        } catch (IOException e) {
            System.out.println("Error while sending message bulk seen status update, error message: " + e.getMessage());
            return false;
        }
    }

    // Method used to send bulk delivered message status update
    public boolean sendBulkDeliveredStatusUpdate(UUID sender, List<UUID> messageIds) {
        SocketEvent<BulkMessageStatusDTO> deliveredUpdateEvent = SocketEvent.<BulkMessageStatusDTO>builder()
                .type(SocketEventType.BULK_STATUS_UPDATE)
                .payload(BulkMessageStatusDTO.builder()
                        .senderId(sender)
                        .messageIds(messageIds)
                        .build())
                .build();
        try{
            output.writeUTF(objectMapper.writeValueAsString(deliveredUpdateEvent));
            lastMessageSent = Instant.now();
            return true;
        } catch (IOException e) {
            System.out.println("Error while sending message bulk delivered status update, error message: " + e.getMessage());
            return false;
        }
    }

    // This method waits for the output handling thread to finish before outputting the final message and closing the program
    public void outputHandlerCleanUp() {
        // Waiting for the ServerOutputHandler thread to finish before giving a chat closed message
        try{
            outputHandler.outputHandlerCleanup();
            outputHandlerThread.join();
            System.out.println("Chat closed!");
        } catch (InterruptedException e) {
            System.out.println("Error while closing server output handler thread");
            throw new RuntimeException(e);
        }
    }

    // This method closes the socket and input/output streams after we finish the messaging
    public void socketCleanUp() {
        try{
            output.close();
            socket.close();
            scheduler.shutdownNow();
        } catch (IOException e){
            System.out.println("WHILE CLOSING CONNECTION");
            throw new RuntimeException(e);
        }
    }

    // Creates a ServerOutputHandler thread which will check for messages from the server
    private Thread createHandlerThread(IncomingDataPersistence incomingDataPersistence) {
        // Initializing the thread used for handling the server output
        outputHandler = null;
        try {
            outputHandler = new ServerOutputHandler(socket.getInputStream(), listener, incomingDataPersistence);
        } catch (IOException e) {
            System.out.println("WHILE CREATING SERVER OUTPUT HANDLER");
            throw new RuntimeException(e);
        }

        // Starting the output handler thread
        Thread thread = new Thread(outputHandler);
        thread.start();
        return thread;
    }

    // Establish a connection
    private void createConnection(String ip, int port) {
        try{
            // Creating ssl socket
            SSLContext sslContext = ClientTls.create(ChatClientGui.PRODUCTION);

            socket = (SSLSocket) sslContext
                    .getSocketFactory()
                    .createSocket(ip, port);

            socket.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});

            SSLParameters parameters = socket.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(parameters);

            //Starting handshake
            socket.startHandshake();

            System.out.println("Connected");

            // Sends output to socket
            output = new DataOutputStream(socket.getOutputStream());
        } catch (UnknownHostException u){
            System.out.println("Unknown host exception");
            throw new RuntimeException(u);
        } catch (IOException e) {
            System.out.println("IOException");
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.println("Unexpected exception occurred");
            throw new RuntimeException(e);
        }
    }

    public void setListener(ChatClientListener listener) {
        this.listener = listener;
        outputHandler.setListener(listener);
    }

    public void setSender(UserDTO sender) {
        this.sender = sender;
    }

}
