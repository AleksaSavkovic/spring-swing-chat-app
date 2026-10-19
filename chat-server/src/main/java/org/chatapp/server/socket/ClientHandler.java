package org.chatapp.server.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.MessageDTO;
import org.chatapp.common.model.UserDTO;
import org.chatapp.common.model.socket.transport.*;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.security.CustomUserDetails;
import org.chatapp.server.security.CustomUserDetailsService;
import org.chatapp.server.security.JwtUtil;
import org.chatapp.server.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ClientHandler implements Runnable{

    private final Socket socket;
    private final ConnectionManager connectionManager;
    private final MessageRouter messageRouter;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    private final AtomicBoolean cleanedUp = new AtomicBoolean(false);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile Instant lastMessageReceived;
    private DataOutputStream output;
    private DataInputStream input;
    private boolean authenticated;
    private UUID userId;

    public ClientHandler (Socket socket, ConnectionManager connectionManager, MessageRouter messageRouter, UserService userService, JwtUtil jwtUtil, CustomUserDetailsService userDetailsService){
        this.socket = socket;
        try {
            socket.setSoTimeout(5000);
            lastMessageReceived = Instant.now();
        } catch (SocketException e) {
            log.error("Error setting unauthenticated socket timeout - stopping client handler.");
            throw new RuntimeException(e);
        }
        this.connectionManager = connectionManager;
        this.messageRouter = messageRouter;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;

        this.objectMapper = new ObjectMapper();
        authenticated = false;
    }

    @Override
    public void run() {
        try {
            // Creating input stream for reading client input
            input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            // Creating output stream for sending data to the client
            output = new DataOutputStream(socket.getOutputStream());

            String messageJson;

            // Reads message from client until "Over" is sent
            while(true){
                try{
                    // Reading socket input
                    messageJson = input.readUTF();
                    lastMessageReceived = Instant.now();

                    // If socket is unauthenticated
                    if (!authenticated) {
                        authenticateSocket(messageJson);
                        // Send user status change indicating that the user has connected
                        messageRouter.handleUserStatusChange(userId, UserStatus.CONNECTED);
                        continue;
                    }

                    // Reading the socket input json and converting it to SocketEvent object
                    // Need to check enum in the socket event and cast it to parametrized generic type.
                    // This can be done before switch(process message) or inside each case statement
                    SocketEvent<?> socketEvent = objectMapper.readValue(messageJson, SocketEvent.class);

                    processMessage(socketEvent);
                } catch (IOException e) {
                    if(e instanceof EOFException || e instanceof SocketException){
                        log.info("User {} closed the chat client at {}", userId != null ? userId : "#unknown", socket.getRemoteSocketAddress());
                        break;
                    } else if (e instanceof SocketTimeoutException) {
                        log.info("Socket idle timeout reached {}", socket.getRemoteSocketAddress(), e);
                        break;
                    }
                    if(!cleanedUp.get()) {
                        log.error("Error occurred while reading client input stream, error message", e);
                        break;
                    }
                } catch (SecurityException | UsernameNotFoundException se) {
                    log.info("User failed to authenticate for socket {}", socket.getRemoteSocketAddress(), se);
                    break;
                }
            }
        } catch (IOException e) {
            if(!cleanedUp.get()){
                cleanup();
            }
            log.error("Error creating socket input output streams!");
        } finally {
            // Perform cleanup if not already performed
            cleanup();
        }
    }

    // Checking message type and processing the message in accordance with its type
    private void processMessage(SocketEvent<?> socketEvent) {
        switch (socketEvent.getType()) {
            case CHAT_MESSAGE -> {
                // Passes on message to the message router which handles delivering the message to the appropriate client handler
                MessageDTO chatMessage = objectMapper.convertValue(
                        socketEvent.getPayload(),
                        MessageDTO.class
                );

                // Check if the sender of the message is the actual user sending the message, otherwise don't send the message
                if(!chatMessage.getSender().getId().equals(userId)) {
                    return;
                }
                messageRouter.routeMessage(chatMessage, null);
            }
            case FIRST_MESSAGE -> {
                FirstMessageDTO firstMessage = objectMapper.convertValue(
                  socketEvent.getPayload(),
                  FirstMessageDTO.class
                );
                // Check if the sender of the message is the actual user sending the message, otherwise don't send the message
                if(!firstMessage.getMessageDTO().getSender().getId().equals(userId)) {
                    return;
                }
                messageRouter.routeMessage(firstMessage.getMessageDTO(), firstMessage.getChatDTO());
            }
            case DELETE_MESSAGE -> {
                MessageDTO deleteMessage = objectMapper.convertValue(
                        socketEvent.getPayload(),
                        MessageDTO.class
                );
                if(!deleteMessage.getSender().getId().equals(userId)) {
                    // If the sender of the message is not the active user stop execution(you can only delete your own messages)
                    return;
                }
                messageRouter.handleMessageDelete(deleteMessage);
            }
            case SEARCH -> {
                // Query the users table for the users with the username similar to the search
                // query and send the results back to the user
                SearchRequestDTO searchRequestDTO = objectMapper.convertValue(
                        socketEvent.getPayload(),
                        SearchRequestDTO.class
                );

                // If the senderId is not the active user stop execution
                if(!searchRequestDTO.getSenderId().equals(userId)) {
                    return;
                }

                if (searchRequestDTO.getSearchQuery() != null && !searchRequestDTO.getSearchQuery().isBlank()) {
                    List<UserDTO> searchResult = userService.searchUsers(searchRequestDTO.getSearchQuery());
                    sendUsers(searchResult);
                }
            }
            case CHATTER_STATUS_CHANGE -> {
                UserStatusChangeDTO userStatusChangeDTO = objectMapper.convertValue(
                        socketEvent.getPayload(),
                        UserStatusChangeDTO.class
                );

                // Check if active userId is equal to userId in user status change (a user can't change another user's status)
                if(userStatusChangeDTO.getUserId() != null && !userStatusChangeDTO.getUserId().equals(userId)) {
                    return;
                }

                if (userStatusChangeDTO.getStatus().equals(UserStatus.LOGOUT)) {
                    cleanup();
                }
            }
            case DELIVERED_MESSAGE -> {
                MessageDTO deliveredMessage = objectMapper.convertValue(
                        socketEvent.getPayload(),
                        MessageDTO.class
                );

                // User cannot set a message delivered status for another user
                if(!deliveredMessage.getSender().getId().equals(userId)) {
                    return;
                }

                messageRouter.handleDeliveredStatusChange(deliveredMessage);
            }
            case SEEN_MESSAGE -> {
                MessageDTO seenMessage = objectMapper.convertValue(
                        socketEvent.getPayload(),
                        MessageDTO.class
                );

                // User cannot set a message seen status for another user
                if(!seenMessage.getSender().getId().equals(userId)) {
                    return;
                }

                messageRouter.handleSeenStatusChange(seenMessage);
            }
            case BULK_STATUS_UPDATE -> {
                BulkMessageStatusDTO bulkMessageStatusDTO = objectMapper.convertValue(
                        socketEvent.getPayload(),
                        BulkMessageStatusDTO.class
                );

                // User cannot set bulk seen or delivered message statuses for another user
                if(!bulkMessageStatusDTO.getSenderId().equals(userId)) {
                    return;
                }

                // If we have a chatId in the bulk dto that means this is a bulk seen update, otherwise it is bulk delivered
                if (bulkMessageStatusDTO.getChatId() != null) {
                    messageRouter.handleBulkSeenChange(bulkMessageStatusDTO.getSenderId(), bulkMessageStatusDTO.getChatId());
                } else {
                    messageRouter.handleBulkDeliveredChange(bulkMessageStatusDTO.getSenderId(), bulkMessageStatusDTO.getMessageIds());
                }
            }
            case PRESENCE_SNAPSHOT -> {
                List<UUID> userIds = objectMapper.convertValue(
                        socketEvent.getPayload(),
                        new TypeReference<List<UUID>>() {}
                );
                sendUserStatuses(userIds);
            }
            case CREATE_GROUP -> {
                ChatDTO groupChat = objectMapper.convertValue(
                        socketEvent.getPayload(),
                        ChatDTO.class
                );
                messageRouter.routeCreateGroupEvent(groupChat);
            }
            case PING_PONG -> {
                sendPong();
            }
            default -> log.info("No message type!");
        }
    }

    // This method authenticates the connected client socket
    private void authenticateSocket(String messageJson) throws SocketException, SecurityException {
        // Json is sent in "Bearer token" format, checking if the format is correct
        if(!messageJson.contains(" ") || messageJson.split(" ").length != 2 || !messageJson.startsWith("Bearer ")){
            throw new SecurityException();
        }
        String token = messageJson.split(" ")[1];

        // Extract user id from JWT
        String idString = jwtUtil.extractId(token);
        UserDetails userDetails = userDetailsService.loadUserById(idString);

        // Before validating the jwt token first check if the user is already connected, if they are reject the connection
        if(connectionManager.getClients().containsKey(UUID.fromString(idString))) {
            log.error("User with id {} is already in the connection manager", UUID.fromString(idString));
            throw new SecurityException("User is not allowed to have multiple active socket connections");
        }

        // Validate jwt
        if (jwtUtil.validateToken(token, (CustomUserDetails) userDetails)) {
            // Remove the client handler from the pending client set as it is authenticated and add it to authenticated client map
            connectionManager.removePendingClient(this);
            userId = ((CustomUserDetails) userDetails).getId();
            connectionManager.addAuthenticatedClient(this);
            authenticated = true;
            socket.setSoTimeout(1800000);
            scheduleActivityCheck();
        }else {
            log.info("Token validation failed for socket: {}", socket.getRemoteSocketAddress());
            throw new SecurityException();
        }
    }

    // Method that initializes a ScheduledExecutorService to run the checkLastActivity method
    private void scheduleActivityCheck() {
        scheduler.scheduleAtFixedRate(this::checkLastActivity, 60, 60, TimeUnit.SECONDS);
    }

    // Method that closes the socket connection if it has been idle for longer than 70 seconds
    private void checkLastActivity() {
        if (Duration.between(lastMessageReceived, Instant.now()).getSeconds() > 70) {
            cleanup();
        }
    }

    // This method sends a snapshot of all user statuses
    private void sendUserStatuses(List<UUID> userIds){
        // Create hash set of user ids based on the received list and use that for searching
        HashSet<UUID> userIdSet = new HashSet<>(userIds);

        // Iterate through connectionManager and check for each client handler if the user is in the list of users
        Set<UUID> onlineUsers = connectionManager.getOnlineFromUsers(userIdSet);
        // Remove the user that requested the presence snapshot as they don't need status information about themselves
        onlineUsers.remove(userId);

        // If there are no online users no need to send a response
        if(onlineUsers.isEmpty()) {
            return;
        }
        // Send PRESENCE_SNAPSHOT with a list of all the users from the received list that are online
        SocketEvent<List<UUID>> socketEvent = SocketEvent.<List<UUID>>builder()
                .type(SocketEventType.PRESENCE_SNAPSHOT)
                .payload(onlineUsers.stream().toList())
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(socketEvent));
        } catch (JsonProcessingException e) {
            log.error("Error converting Presence Snapshot to JSON");
        } catch (IOException e) {
            log.error("Error send the presence snapshot to the user socket.");
            throw new RuntimeException(e);
        }
    }

    // Send the user status change to all chatters for which it is of interest (mutual chat users)
    public void sendUserStatusChange(UUID userId, UserStatus change){
        // Send CHATTER_STATUS_CHANGE message with the userId as the senderId and the content as "disconnected/connected"
        SocketEvent<UserStatusChangeDTO> socketEvent = SocketEvent.<UserStatusChangeDTO>builder()
                .type(SocketEventType.CHATTER_STATUS_CHANGE)
                .payload(UserStatusChangeDTO.builder()
                        .userId(userId)
                        .status(change)
                        .build())
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(socketEvent));
        } catch (JsonProcessingException e) {
            log.error("Error converting Chatter status change message for user {} to JSON", userId);
        } catch (IOException e) {
            log.error("Error send the status change message for user {} to the user socket.", userId);
            throw new RuntimeException(e);
        } catch (NotFoundException e) {
            log.error("Error sending the status change for user {}, user could not be found", userId);
        }
    }

    // Method that writes the message json to the socket output stream
    // (sends regular messages, group chat messages, delete messages, single status updates)
    public void sendMessage(MessageDTO message, SocketEventType eventType) {
        if (socket.isClosed()) {
            log.warn("Attempted to send message to closed socket: {}, for user: {}", socket.getRemoteSocketAddress(), userId);
            cleanup();
            return;
        }
        SocketEvent<MessageDTO> socketEvent = SocketEvent.<MessageDTO>builder()
                .type(eventType) // This handles non chat_message type messages that can get send here like delivered status message being sent from message router
                .payload(message)
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(socketEvent));
        } catch (IOException e) {
            log.error("Error sending a message from user: {}, to user: {}", message.getSender().getUsername(), userId);
            throw new RuntimeException(e);
        }
    }

    // Method that writes the first message dto as json to the socket output stream
    public void sendFirstMessage(FirstMessageDTO firstMessageDTO) {
        SocketEvent<FirstMessageDTO> socketEvent = SocketEvent.<FirstMessageDTO>builder()
                .type(SocketEventType.FIRST_MESSAGE)
                .payload(firstMessageDTO)
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(socketEvent));
        } catch (IOException e) {
            log.error("Error sending first message in regular chat from user: {}, to user: {}", firstMessageDTO.getMessageDTO().getSender().getId(), userId);
            throw new RuntimeException(e);
        }
    }

    // Method that writes the chat dto of the created group as json to the socket output stream
    public void sendCreateGroup(ChatDTO groupChat){
        SocketEvent<ChatDTO> createdEvent = SocketEvent.<ChatDTO>builder()
                .type(SocketEventType.CREATE_GROUP)
                .payload(groupChat)
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(createdEvent));
        } catch (IOException e) {
            log.error("Error sending created group chat: {} to user: {}", groupChat.getId(), userId);
            throw new RuntimeException(e);
        }
    }

    // Method that writes the user search results as json to the socket output stream
    public void sendUsers(List<UserDTO> users) {
        SocketEvent<List<UserDTO>> socketEvent = SocketEvent.<List<UserDTO>>builder()
                .type(SocketEventType.SEARCH)
                .payload(users)
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(socketEvent));
        } catch (IOException e) {
            log.error("Error sending user search result from server to user: {}", userId);
            throw new RuntimeException(e);
        }
    }

    // Make message content BULK so and type SEEN_MESSAGE
    // Send message status sent for all chat messages for a user, usually when user opens chat
    public void sendBulkSeenStatus(UUID senderId, UUID chatId) {
        SocketEvent<BulkMessageStatusDTO> socketEvent = SocketEvent.<BulkMessageStatusDTO>builder()
                .type(SocketEventType.BULK_STATUS_UPDATE)
                .payload(BulkMessageStatusDTO.builder()
                        .senderId(senderId)
                        .chatId(chatId)
                        .build())
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(socketEvent));
        } catch (IOException e) {
            log.error("Error sending bulk seen status change to user {}", senderId, e);
        }
    }

    // Sends message status delivered for a list of messages for a user that became online
    public void sendBulkDeliveredStatus(UUID senderId, List<UUID> messageIds) {
        SocketEvent<BulkMessageStatusDTO> socketEvent = SocketEvent.<BulkMessageStatusDTO>builder()
                .type(SocketEventType.BULK_STATUS_UPDATE)
                .payload(BulkMessageStatusDTO.builder()
                        .senderId(senderId)
                        .messageIds(messageIds)
                        .build())
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(socketEvent));
        } catch (JsonProcessingException e) {
            log.error("Error writing message list in bulk delivered status change for user {}", senderId, e);
        } catch (IOException e) {
            log.error("Error sending bulk delivered status change to user {}", senderId, e);
        }
    }

    // Method that sends the response to the client after it pings
    private void sendPong() {
        SocketEvent<String> socketEvent = SocketEvent.<String>builder()
                .type(SocketEventType.PING_PONG)
                .payload(Instant.now().toString())
                .build();
        try {
            output.writeUTF(objectMapper.writeValueAsString(socketEvent));
        } catch (JsonProcessingException e) {
            log.error("Error writing ping-pong socket event", e);
        } catch (IOException e) {
            log.error("Error sending ping-pong message event", e);
        }
    }

    // Perform this so messages are not being sent to this socket
    private void cleanup() {
        if (!cleanedUp.compareAndSet(false, true)) {
            log.info("cleanup() already called — skipping");
            return;
        }
        log.info("Closing connection with: {}", socket.getRemoteSocketAddress());
        scheduler.shutdownNow();

        // Check if user is authenticated and based on that select the proper remove client method
        if(userId == null) {
            if(connectionManager.removePendingClient(this)) {
                log.info("Client removed from the connection manager successfully!");
            } else {
                log.error("Error removing pending client from connection manager!");
            }
        } else {
            if(connectionManager.removeAuthenticatedClient(this) != null) {
                messageRouter.handleUserStatusChange(userId, UserStatus.DISCONNECTED);
                log.info("Client with id {} removed from the connection manager successfully!", userId);
            }else {
                log.error("Error removing authenticated client from connection manager!");
            }
        }

        // Close the socket and the input/output streams
        try {
            if (!socket.isClosed()) {
                socket.close();
                input.close();
                output.close();
            }
        } catch (IOException e) {
            log.info("Error closing socket", e);
        }

        log.info("ClientHandler cleaned up (once) for client {}.", userId != null ? userId : "#unknown");
    }

    private Socket getSocket() {
        return socket;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public UUID getUserId() {
        return userId;
    }

}
