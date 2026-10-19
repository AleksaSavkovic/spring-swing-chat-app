package org.chatapp.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chatapp.client.interfaces.ChatClientListener;
import org.chatapp.common.model.*;
import org.chatapp.common.model.socket.transport.*;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerOutputHandler implements Runnable{

    private final DataInputStream input;
    private ChatClientListener listener;
    private final ObjectMapper objectMapper;
    private final IncomingDataPersistence incomingDataPersistence;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Instant lastMessageReceived;

    public ServerOutputHandler (InputStream inputStream, ChatClientListener listener, IncomingDataPersistence incomingDataPersistence){
        this.input = new DataInputStream(new BufferedInputStream(inputStream));
        this.listener = listener;
        this.objectMapper = new ObjectMapper();
        this.incomingDataPersistence = incomingDataPersistence;
        schedulePongCheck();
    }

    @Override
    public void run() {
        try(input) {
            SocketEvent<?> socketEvent;
            // Reads and displays the messages from the socket input stream (Server messages to Client)
            while(true){
                socketEvent = objectMapper.readValue(input.readUTF(), SocketEvent.class);
                lastMessageReceived = Instant.now();

                switch (socketEvent.getType()){
                    case CHAT_MESSAGE -> {
                        // Save the received message into the database
                        MessageDTO chatMessage = objectMapper.convertValue(socketEvent.getPayload(), MessageDTO.class);
                        UUID replyId = chatMessage.getReplyToId();
                        // If reply is not null and can't be found in local database remove the reply before saving
                        // message to local database
                        if(chatMessage.getReplyToId() != null &&
                                !incomingDataPersistence.findMessageByIdInLocal(chatMessage.getReplyToId())) {
                            replyId = chatMessage.getReplyToId();
                            chatMessage.setReplyToId(null);
                        }
                        MessageDTO savedMessage = incomingDataPersistence.insertNewMessage(chatMessage);

                        // If replyId saved in the variable is different from the reply id in chatMessage it means
                        // we have saved the message without the replyId and we need to set it back in the dto before
                        // sending it to the event listener
                        if(replyId != null) {
                            savedMessage.setReplyToId(replyId);
                        }

                        // Trigger event in ui
                        listener.onMessageReceived(savedMessage);
                    }
                    case FIRST_MESSAGE -> {
                        FirstMessageDTO firstMessageDTO = objectMapper.convertValue(socketEvent.getPayload(), FirstMessageDTO.class);
                        // Save the new chat first if not already in user local db
                        incomingDataPersistence.saveChatIfNotInLocal(firstMessageDTO.getChatDTO());

                        // Save the received message into the database
                        MessageDTO savedMessage = incomingDataPersistence.insertNewMessage(firstMessageDTO.getMessageDTO());

                        // Trigger event in ui
                        listener.onMessageReceived(savedMessage);
                    }
                    case DELETE_MESSAGE -> {
                        // Set the content of the message in the database to null
                        MessageDTO deletedMessage = objectMapper.convertValue(socketEvent.getPayload(), MessageDTO.class);
                        incomingDataPersistence.deleteMessage(deletedMessage.getId());

                        // Trigger event in ui
                        listener.onDeleteMessage(deletedMessage);
                    }
                    case CREATE_GROUP -> {
                        ChatDTO groupChat = objectMapper.convertValue(socketEvent.getPayload(), ChatDTO.class);
                        // Save the new chat first if not already in user local db(it shouldn't be in local at this point)
                        incomingDataPersistence.saveChatIfNotInLocal(groupChat);

                        // Trigger event in ui
                        listener.onGroupCreated(groupChat);
                    }
                    case SEARCH -> {
                        List<UserDTO> users = objectMapper.convertValue(socketEvent.getPayload(), new TypeReference<>(){});
                        // Trigger event in ui
                        listener.onSearchResultReceived(users);
                    }
                    case PRESENCE_SNAPSHOT -> {
                        List<UUID> onlineUserIds = objectMapper.convertValue(socketEvent.getPayload(), new TypeReference<>() {});
                        listener.onPresenceSnapshotReceived(onlineUserIds);
                    }
                    case CHATTER_STATUS_CHANGE -> {
                        UserStatusChangeDTO userStatusChangeDTO = objectMapper.convertValue(socketEvent.getPayload(), UserStatusChangeDTO.class);
                        // Trigger event in ui
                        listener.onChatterStatusChange(userStatusChangeDTO.getUserId(), userStatusChangeDTO.getStatus());
                    }
                    case SEEN_MESSAGE -> {
                        MessageDTO seenMessage = objectMapper.convertValue(socketEvent.getPayload(), MessageDTO.class);
                        // Update status in database
                        incomingDataPersistence.singleStatusUpdate(seenMessage.getSender().getId(), seenMessage.getId(), StatusType.SEEN);
                        // Trigger event in ui
                        listener.onSingleMessageStatusChange(seenMessage, SocketEventType.SEEN_MESSAGE);
                    }
                    case DELIVERED_MESSAGE -> {
                        MessageDTO deliveredMessage = objectMapper.convertValue(socketEvent.getPayload(), MessageDTO.class);
                        // Update status in database
                        incomingDataPersistence.singleStatusUpdate(deliveredMessage.getSender().getId(), deliveredMessage.getId(), StatusType.DELIVERED);
                        // Trigger event in ui
                        listener.onSingleMessageStatusChange(deliveredMessage, SocketEventType.DELIVERED_MESSAGE);
                    }
                    case BULK_STATUS_UPDATE -> {
                        // Do bulk status handling
                        BulkMessageStatusDTO bulkMessageStatusDTO = objectMapper.convertValue(socketEvent.getPayload(), BulkMessageStatusDTO.class);
                        // If we have a chatId in the bulk dto that means this is a bulk seen update, otherwise it is bulk delivered
                        if(bulkMessageStatusDTO.getChatId() != null) {
                            // Update all statuses in db
                            incomingDataPersistence.bulkSeenUpdate(bulkMessageStatusDTO.getSenderId(), bulkMessageStatusDTO.getChatId());
                            // Trigger event in ui
                            listener.onBulkSeenStatusChange(bulkMessageStatusDTO.getChatId());
                        } else {
                            // Update statuses in the database
                            incomingDataPersistence.bulkDeliveredUpdate(bulkMessageStatusDTO.getSenderId(), bulkMessageStatusDTO.getMessageIds());
                            // Trigger event in ui
                            listener.onBulkDeliveredStatusChange(bulkMessageStatusDTO.getMessageIds());
                        }
                    }
                    case SENT_MESSAGE -> {
                        MessageDTO sentMessage = objectMapper.convertValue(socketEvent.getPayload(), MessageDTO.class);
                        // Save to local
                        MessageDTO savedMessage = incomingDataPersistence.insertNewMessage(sentMessage);

                        // Trigger event in ui
                        listener.onSingleMessageStatusChange(savedMessage, SocketEventType.SENT_MESSAGE);
                    }
                    case PING_PONG -> {
                        String pongTimeString = objectMapper.convertValue(socketEvent.getPayload(), String.class);
                        System.out.println("Received pong at: " + pongTimeString);
                    }
                    default -> System.out.println("No message type");
                }
            }
        } catch (IOException e) {
            if(e instanceof EOFException){
                listener.onServerError("UNEXPECTED SERVER ERROR");
            }else if(!e.getMessage().equals("Socket closed")){
                listener.onServerError("Connection error: " + e.getMessage());
            }
        }

    }

    // Method that initializes a ScheduledExecutorService to run the pingIfNoActivity method
    private void schedulePongCheck() {
        scheduler.scheduleAtFixedRate(this::disconnectIfNoActivity, 30, 30, TimeUnit.SECONDS);
    }

    // Pings the server if there hasn't been a message in 30 seconds
    private void disconnectIfNoActivity() {
        if(Duration.between(lastMessageReceived, Instant.now()).getSeconds() > 70) {
            // Notify the user that the connection with the server has been lost, and move them back to the login page
            listener.onDisconnected();
        }
    }

    // Sets the listener which gets notified about received messages from the server
    public void setListener(ChatClientListener listener) {
        this.listener = listener;
    }

    // Handle the closing of the server output handler
    public void outputHandlerCleanup(){
        scheduler.shutdownNow();
        try {
            input.close();
        } catch (IOException e) {
            System.out.println("Error closing input stream in server output handler");
        }
    }
}
