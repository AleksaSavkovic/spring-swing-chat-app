package org.chatapp.client.service;

import org.chatapp.client.interfaces.GetMessageAttachmentExecutor;
import org.chatapp.client.repository.*;
import org.chatapp.client.service.security.JwtToken;
import org.chatapp.client.service.security.KeyStoreManager;
import org.chatapp.client.service.security.TokenStorage;
import org.chatapp.client.interfaces.ChatClientListener;
import org.chatapp.client.ui.ChatClientGui;
import org.chatapp.client.ui.MainFrame;
import org.chatapp.client.ui.records.MessageStatusDisplay;
import org.chatapp.common.model.*;
import org.chatapp.common.model.socket.transport.SocketEventType;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ChatController implements GetMessageAttachmentExecutor {
    // Socket and http clients for communicating with their respective servers
    private ChatClient socketClient;
    private final ChatHttpClient httpClient;

    // Secret key and refresh token storage
    private static final String KEYSTORE_FILE = "my_keystore.p12";
    private static final String TOKEN_FILE = "refresh_token.enc";
    private static final String PASSWORD_FILE = "device-password";
    private final KeyStoreManager keyManager;
    private final TokenStorage tokenStorage;

    // Atomic reference used for the access token to ensure atomicity while being used by multiple threads
    private final AtomicReference<JwtToken> accessToken;
    // ScheduledExecutorService used for scheduling the token refresh when the token expiry time passes
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Flag used for checking outside this class if a new refresh token needs to be obtained
    public boolean hasRefreshToken = false;

    // Database repositories
    private ChatRepository chatRepository;
    private ChatMemberRepository chatMemberRepository;
    private MessageRepository messageRepository;
    private SyncRepository syncRepository;
    private DatabaseManager databaseManager;

    // Event listener for notifying the gui thread of changes to the local database
    private ChatClientListener uiListener;

    public ChatController(ChatHttpClient httpClient){
        this.httpClient = httpClient;

        // Initializing our keystore manager for our secret and our token storage class for reading and
        // writing to the encrypted refresh token file
        keyManager = new KeyStoreManager(AppPaths.getAppDataDirectory().resolve(KEYSTORE_FILE).toString());
        tokenStorage = new TokenStorage(AppPaths.getAppDataDirectory().resolve(TOKEN_FILE).toString(), keyManager);
        accessToken = new AtomicReference<>(null);

        try {
            String token = tokenStorage.loadRefreshToken();
            if(token != null){
                // In case we are able to get a new access token with our refresh token read from file, we just set up
                // the scheduling of the refresh method for further access token requests. Otherwise, user has to log in manually
                if (refreshAccessToken()){
                    hasRefreshToken = true;
                    setupRefreshScheduling();
                } else {
                    System.out.println("Refresh request failed. Refresh token expired or invalid.");
                }
            }else {
                System.out.println("Refresh token file not found");
            }
        } catch (Exception e) {
            System.out.println("Error reading refresh token file: " + e.getMessage());
        }
    }

    // Socket connection is established afterward when the user authenticates
    public void setSocketClient(ChatClient socketClient) {
        if(this.socketClient == null) {
            this.socketClient = socketClient;
        }
    }

    // Once a database initializer is provided local database repositories are created
    public void setDatabaseRepos(DatabaseManager databaseManager,
                                 ChatRepository chatRepository,
                                 ChatMemberRepository chatMemberRepository,
                                 MessageRepository messageRepository,
                                 SyncRepository syncRepository){
        this.databaseManager = databaseManager;
        this.chatRepository = chatRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.messageRepository = messageRepository;
        this.syncRepository = syncRepository;
    }

    public void performSocketCleanUp() {
        //Close the connection
        socketClient.socketCleanUp();
        // Wait for the output handler to finish and then show the user he closed the chat
        socketClient.outputHandlerCleanUp();
    }

    // Sets the sender user and event listener objects
    public void socketClientSetup(UserDTO sender, MainFrame listener){
        socketClient.setSender(sender);
        socketClient.setListener(listener);
        // Set the listener object in chat controller so it can be used for firing events to the UI
        uiListener = listener;
    }

    // Returns all chats for the user
    public List<ChatDTO> loadChats(UUID activeUser) {
        // Get chats from local
        List<ChatDTO> localChats = getChatsLocal(activeUser);

        // Run sync in the background
        CompletableFuture.runAsync(() -> syncData(activeUser));

        // Return local results
        return localChats;
    }

    // Gets chats for local user from the local database
    public List<ChatDTO> getChatsLocal(UUID activeUser) {
        return databaseManager.executeQuery(() -> chatRepository.getUserChats(activeUser));
    }

    // Finds the chat from local db by id
    public ChatDTO findChatById(UUID chatId) {
        return databaseManager.executeQuery(() -> chatRepository.findChatByIdWithUsers(chatId));
    }

    // Get sync data, persist it to the database and fire off sync event if any date was updated
    private void syncData(UUID activeUser) {
        boolean updated = false;
        // Get the last sync time if the application was synced before
        Instant lastSync = databaseManager.executeQuery(() -> syncRepository.getLastSync());
        // Send sync request via http client
        SyncResponseDTO syncResponse = httpClient.sync(activeUser, lastSync, accessToken.get().getTokenEncoded());

        // Save the results in the local database, check if there were any changes to chats/messages before persisting them
        if(syncResponse.getNewChats() != null && !syncResponse.getNewChats().isEmpty()) {
            databaseManager.executeTransaction(() -> syncRepository.batchInsertChats(syncResponse.getNewChats()));
            updated = true;
        }

        // Add all the referenceOnly messages to the database first (if there are any)
        if(!syncResponse.getReferenceOnlyMessages().isEmpty()) {
            databaseManager.executeTransaction(() ->
                    syncRepository.batchInsertReferenceOnlyMessage(syncResponse.getReferenceOnlyMessages()));
        }

        // Handle the new synced messages
        if(syncResponse.getNewMessages() != null && !syncResponse.getNewMessages().isEmpty()) {
            // Create the list which will store the message ids whose statuses needs to be set as delivered for this user
            List<UUID> toSetDelivered = getDeliveredMessageUuids(activeUser, syncResponse.getNewMessages());
            // Set the status for all messages received by the user as delivered if they are SENT (because they must be delivered if they arrived to user)
            List<MessageDTO> toSaveMessages = new ArrayList<>(syncResponse.getNewMessages());
            toSaveMessages.stream()
                    .flatMap(messageDTO -> messageDTO.getStatuses().stream())
                    .filter(messageStatusDTO ->
                            messageStatusDTO.getUserId().equals(activeUser) &&
                            messageStatusDTO.getStatus().equals(StatusType.SENT))
                    .forEach(messageStatusDTO -> messageStatusDTO.setStatus(StatusType.DELIVERED));

            // Make sure sent times are ascending so we don't get foreign key constraints with replies in the message list
            toSaveMessages.sort(Comparator.comparing(MessageDTO::getSentTime));

            // Insert the messages to local database
            databaseManager.executeTransaction(() -> syncRepository.batchInsertMessage(toSaveMessages));
            // Update all the denormalized chat data where needed
            updateChatDataAfterMessageSync(activeUser, syncResponse);
            // Send the bulk delivered update over the socket
            bulkUpdateDeliveredStatus(activeUser, toSetDelivered);
            updated = true;
        }

        // Save the sync time in the sync table
        databaseManager.executeTransaction(() -> syncRepository.insertSync(syncResponse.getServerTime()));

        // Fire of event to ui event listener if the chat sidebar needs to be refreshed. Probably could change method to void
        if(updated){
            // Fire the event
            uiListener.onChatsSync();
        }
    }

    // This method updates all the denormalized chat data for the newly received messages
    private void updateChatDataAfterMessageSync(UUID activeUser, SyncResponseDTO syncResponse) {
        // Group received messages by chat id
        Map<UUID, List<MessageDTO>> messageChatMap = syncResponse.getNewMessages().stream().collect(Collectors.groupingBy(MessageDTO::getChatId));

        // Remove messages with chat ids which are present in newChats
        if(syncResponse.getNewChats() != null && !syncResponse.getNewChats().isEmpty()) {
            for(ChatDTO chat : syncResponse.getNewChats()){
                if(messageChatMap.get(chat.getId()) != null) {
                    messageChatMap.remove(chat.getId());
                }
            }
        }

        // Go through chat map, and update the chat info where needed
        for(Map.Entry<UUID, List<MessageDTO>> entry : messageChatMap.entrySet()) {
            UUID chatId = entry.getKey();
            List<MessageDTO> chatMessages = entry.getValue();

            // Get the chatDTO from database based by chat id
            ChatDTO chat = databaseManager.executeQuery(() -> chatRepository.findChatById(chatId));

            // Find the last message out of new messages
            MessageDTO latest = chatMessages.stream()
                    .max(Comparator.comparing(MessageDTO::getSentTime))
                    .orElse(null);

            // Calculate the new unread count from the database
            int unreadCount = databaseManager.executeQuery(() -> chatRepository.recalculateChatUnreadCount(chatId, activeUser));

            // If the latest of the new messages is after the last message sent time in the chat update the chat
            if(latest!= null &&
                    (chat.getLastMessageSentTime() == null ||
                            chat.getLastMessageSentTime().isBefore(latest.getSentTime()))) {
                chat.setLastMessageSentTime(latest.getSentTime());
                chat.setLastMessageContent(latest.getContent());
                chat.setUnreadMessages(unreadCount);

                // Update the chat in the chat repository with the new information
                databaseManager.executeTransaction(() -> chatRepository.updateChatInfo(chat));
            }
        }
    }

    // Get the message UUIDS for all messages for which DELIVERED status update needs to be sent
    private List<UUID> getDeliveredMessageUuids(UUID activeUser, List<MessageDTO> messages) {
        List<UUID> toSetDelivered = new LinkedList<>();
        // Going through newly received messages and adding the message if it has a user status SENT for active user
        for(MessageDTO message : messages) {
            for(MessageStatusDTO statusDTO : message.getStatuses()) {
                if(statusDTO.getStatus().equals(StatusType.SENT) && statusDTO.getUserId().equals(activeUser)){
                    toSetDelivered.add(message.getId());
                }
            }
        }
        return toSetDelivered;
    }

    // Method that calls the insert sync method from sync repository
    public void insertSync(){
        databaseManager.executeTransaction(() -> syncRepository.insertSync(Instant.now()));
    }

    // Uploads the file as multipart file to the http server
    public String sendMessageWithAttachment(Path path, ChatDTO chatDTO, UUID replyId) {
        String filename = uploadFileToServer(path);
        return sendMessage(filename, chatDTO, MessageType.FILE_MESSAGE, replyId) ? filename : null;
    }

    // Uploads the file as multipart file to the http server
    public String sendFirstMessageWithAttachment(Path path, ChatDTO chatDTO) {
        String filename = uploadFileToServer(path);

        return sendFirstMessage(filename, chatDTO, MessageType.FILE_MESSAGE) ? filename : null;
    }

    // Deletes the messages by sending the delete message event to the socket server
    public boolean deleteMessage(UUID messageId) {
        MessageDTO message = databaseManager.executeQuery(() -> messageRepository.findMessageById(messageId));
        return socketClient.sendDeleteMessage(message);
    }

    // Separate this http stuff, call it after finding out the message is null in mainframe
    // Finds the message in the local database by id
    public MessageDTO findMessageById(UUID messageId) {
        if(messageId == null) {
            return null;
        }
        return databaseManager.executeQuery(() -> messageRepository.findMessageById(messageId));
    }

    // Get the replyToId from the message from remote database. Then insert it into local and update the replyToId in
    // the local database for the message referencing the replyId.
    public void getReplyFromRemote(MessageDTO messageWithReply) {
        if(messageWithReply != null && messageWithReply.getReplyToId() != null) {
            // Fetch the message from remote server via http client in the background
            CompletableFuture.supplyAsync(() ->
                    {
                        try {
                            // Get message from remote
                            MessageDTO remoteMessage = httpClient.findMessageById(messageWithReply.getReplyToId(), accessToken.get().getTokenEncoded());
                            // Save message to local database (set it as reference only to true as we cannot guarantee full message history up to it)
                            boolean localMessage = databaseManager.executeQuery(() -> messageRepository.insertMessage(remoteMessage, true));
                            // If message is saved successfully to local update the reply to id of the message in local
                            if(localMessage) {
                                databaseManager.executeTransaction(() -> messageRepository
                                        .updateMessageReplyToId(messageWithReply.getId(), messageWithReply.getReplyToId()));
                            }

                            // Get message image from remote if image is a file message
                            if(remoteMessage.getMessageType().equals(MessageType.FILE_MESSAGE)) {
                                getMessageAttachment(messageWithReply.getReplyToId());
                            }
                            return remoteMessage;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    // Notify the uiListener in mainframe that the reply has been loaded from remote
            ).thenAccept((messageDTO) -> uiListener.onReplyLoaded(messageDTO));
        }
    }

    // Returns all messages from local database whose id is in the given list
    public List<MessageDTO> getLocalMessagesWhereIdIn(List<UUID> ids) {
        // Return empty list if there are no ids to get
        if(ids.isEmpty()) {
            return List.of();
        }
        return databaseManager.executeQuery(() -> messageRepository.findMessagesByIdIn(ids));
    }

    private String uploadFileToServer(Path path) {
        HttpResponse<?> response = httpClient.sendImageFilePostRequest(path, accessToken.get().getTokenEncoded());

        String filename = response.headers().firstValue("Location").orElseThrow(() ->
                new IllegalArgumentException("No saved filename returned from remote server"));

        filename = filename.substring(filename.lastIndexOf("/") + 1);
        return filename;
    }


    // Get image file from remote server, to be used when the file is not there locally already
    @Override
    public boolean getMessageAttachment(UUID messageId) {
        HttpResponse<byte[]> response = httpClient.getMessageAttachment(messageId, accessToken.get().getTokenEncoded());
        // If we don't get a successful response return false
        if(response == null) {
            return false;
        }
        // Extracting the filename from headers
        String contentDisposition = response.headers()
                .firstValue("Content-Disposition")
                .orElseThrow(() -> new IllegalStateException(
                        "Missing Content-Disposition header"
                ));
        String filename = contentDisposition
                .substring(contentDisposition.indexOf("filename=") + 9)
                .replace("\"", "");

        // Building file destination path and saving the file there
        Path destination = Path.of(
                String.valueOf(ChatClientGui.LOCAL_FILES_PATH.resolve("images")),
                filename
        );
        try {
            Files.write(destination, response.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    // Save file to the user files directory
    public boolean saveUserFile(String filename, Path sourcePath) {
        Path destinationPath = ChatClientGui.LOCAL_FILES_PATH.resolve("images").resolve(filename);
        try {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save file: " + e.getMessage());
            return false;
        }
    }

    // Sends the message over the socket
    public boolean sendMessage(String messageContent, ChatDTO chatDTO, MessageType messageType, UUID replyId){
        return socketClient.sendMessage(messageContent, chatDTO, messageType, replyId);
    }

    // Sends the first message alongside the chatDTO
    public boolean sendFirstMessage(String messageContent, ChatDTO chatDTO, MessageType messageType) {
        return socketClient.sendFirstMessage(messageContent, chatDTO, messageType);
    }

    // Sends the user search query through the socket
    public boolean sendSearchQuery(String query){
        return socketClient.sendSearchQuery(query);
    }

    // Send the status update message to notify other users of a message status change
    public boolean sendStatusUpdateMessage(MessageDTO statusMessage){
        // Create the status change dto based on the message being sent through socket
        MessageStatusChangeDTO statusUpdateData = MessageStatusChangeDTO.builder()
                .userId(statusMessage.getSender().getId())
                .messageId(statusMessage.getId())
                .status(StatusType.SEEN) // For now this method is being used only for sending the seen status update
                .build();
        // Save the updated status to local first
        databaseManager.executeTransaction(() -> messageRepository.updateMessageStatus(statusUpdateData));

        // Send the status update message through the socket client
        return socketClient.sendStatusUpdateMessage(statusMessage, SocketEventType.SEEN_MESSAGE);
    }

    // Set the unread count for chat to 0 in the local database
    public void resetUnreadCount(ChatDTO chat) {
        ChatDTO updateData = ChatDTO.builder()
                .id(chat.getId())
                .lastMessageSentTime(chat.getLastMessageSentTime())
                .lastMessageContent(chat.getLastMessageContent())
                .unreadMessages(0)
                .build();

        databaseManager.executeTransaction(() -> chatRepository.updateChatInfo(updateData));
    }

    // Update the last message data and the unread count for the chat
    public void updateChatData(ChatDTO chat) {
        ChatDTO updateData = ChatDTO.builder()
                .id(chat.getId())
                .lastMessageSentTime(chat.getLastMessageSentTime())
                .lastMessageContent(chat.getLastMessageContent())
                .unreadMessages(chat.getUnreadMessages())
                .build();

        databaseManager.executeTransaction(() -> chatRepository.updateChatInfo(updateData));
    }

    // Send the bulk seen status update for the chat which the user opened, and also update the statuses in local database
    public boolean bulkUpdateSeenStatus(UserDTO activeUser, UUID activeChat) {
        // Save the seen update status to the local database before sending it to remote
        databaseManager.executeTransaction(() -> messageRepository.bulkSeenUpdate(activeUser.getId(), activeChat));

        // Send the bulk seen status update through the socket client
        return socketClient.sendBulkSeenStatusUpdate(activeUser, activeChat);
    }

    // Send the bulk delivered status update for each chat which the user opened, and also update the statuses in local database
    public boolean bulkUpdateDeliveredStatus(UUID activeUser, List<UUID> messageIds) {
        return socketClient.sendBulkDeliveredStatusUpdate(activeUser, messageIds);
    }

    // Send the auth message needed to authenticate user socket connection
    public boolean sendAuthMessage() {
        return socketClient.sendAuthMessage(accessToken.get().getTokenEncoded());
    }

    // Send the request for presence snapshot for all connected user online statuses
    public boolean sendPresenceSnapRequest(Set<UUID> userIds) {
        return socketClient.sendPresenceSnapRequest(userIds);
    }

    // Get messages from specified chat, sync statuses only if the page number is one and syncStatuses boolean is true
    public List<MessageDTO> getChatMessages(UUID chatId, int pageNumber, int pageSize, boolean syncStatuses) throws SQLException {
        // If the first page of the chat is requested run the sync method which gets all new/updated message statuses from remote
        if(pageNumber == 1 && syncStatuses) {
            CompletableFuture.runAsync(() -> syncChatMessageStatuses(chatId));
        }

        ArrayList<MessageDTO> fromLocal =  databaseManager.executeQuery(() -> messageRepository.getChatMessages(chatId, pageNumber, pageSize));

        // If fromLocal is equal to null an error happened in the local db and we return null
        if(fromLocal == null) {
            throw new SQLException("Error occurred trying to fetch chat messages from local!");
        }

        // If fromLocal is empty it means that there are no more chat messages in the local database
        if(!fromLocal.isEmpty()){
            // If we get less then the full page from local we try to make it a full page by querying remote
            if(fromLocal.size()<pageSize) {
                List<MessageDTO> messagesFromRemote = httpClient
                        .getChatMessages(chatId, pageNumber, pageSize, accessToken.get().getTokenEncoded());
                // If messages from remote is empty just return from local
                if(messagesFromRemote == null || messagesFromRemote.isEmpty()) {
                    return fromLocal;
                }

                // Remove duplicates from remote
                messagesFromRemote.removeIf(fromLocal::contains);

                // Get all reference-only messages and save them to local
                List<MessageDTO> referenceOnlyMessages = fetchMissingReplies(messagesFromRemote);
                databaseManager.executeTransaction(() -> syncRepository.batchInsertReferenceOnlyMessage(referenceOnlyMessages));

                // Save non duplicates to local database and re-read the page from local database together with the new additions
                databaseManager.executeTransaction(() -> syncRepository.batchInsertMessage(messagesFromRemote));
                fromLocal = databaseManager.executeQuery(() -> messageRepository.getChatMessages(chatId, pageNumber, pageSize));
            }
            return fromLocal;
        }

        // Get messages from server if there are no more in local
        List<MessageDTO> messagesFromRemote = httpClient.getChatMessages(chatId, pageNumber, pageSize, accessToken.get().getTokenEncoded());

        // If we get new messages from server we persist them to the local database and then show them to the user
        if(messagesFromRemote != null && !messagesFromRemote.isEmpty()) {
            // Get all reference-only messages and save them to local
            List<MessageDTO> referenceOnlyMessages = fetchMissingReplies(messagesFromRemote);
            databaseManager.executeTransaction(() -> syncRepository.batchInsertReferenceOnlyMessage(referenceOnlyMessages));

            // Insert the actual remote messages
            databaseManager.executeTransaction(() -> syncRepository.batchInsertMessage(messagesFromRemote));
            return databaseManager.executeQuery(() -> messageRepository.getChatMessages(chatId, pageNumber, pageSize));
        }
        return messagesFromRemote;
    }

    // Gets all the reply id's not in the message dto list
    private List<MessageDTO> fetchMissingReplies(List<MessageDTO> messagesFromRemote) {
        // Get a set of message ids from remote to be saved into local
        Set<UUID> messageIds = messagesFromRemote.stream()
                .map(MessageDTO::getId)
                .collect(Collectors.toSet());

        // Get a set of all the reply ids to which the remote messages point to which are not in local or in the
        //  set of remote messages
        Set<UUID> missingReplyIds = messagesFromRemote.stream()
                .map(MessageDTO::getReplyToId)
                .filter(Objects::nonNull)
                .filter(replyId -> !messageIds.contains(replyId))
                .collect(Collectors.toSet());

        // Get all the missing replies messages from remote and save them to local
        return httpClient.findMessagesWhereIdIn(missingReplyIds, accessToken.get().getTokenEncoded());
    }

    //  Get the updated chat statuses from remote
    public void syncChatMessageStatuses(UUID chatId){
        boolean updated = false;
        // Get the first message time from the database
        Instant firstMsg = databaseManager.executeQuery(() -> messageRepository.getOldestSentTimeChatMsg(chatId));
        // Nothing to sync if there are no messages
        if(firstMsg == null) {
            return;
        }
        // Get the last sync time from the messages
        Instant lastSync = databaseManager.executeQuery(() -> syncRepository.getLastStatusSync(chatId));

        // Send the http request
        List<MessageStatusChangeDTO> statusUpdates = httpClient.syncStatuses(chatId, firstMsg, lastSync, accessToken.get().getTokenEncoded());
        // Insert new status sync to the database for chat
        databaseManager.executeTransaction(() -> syncRepository.insertStatusSync(chatId, Instant.now()));
        if(statusUpdates!= null && !statusUpdates.isEmpty()){
            // Persist the result into the local database
            databaseManager.executeTransaction(() -> syncRepository.batchUpdateStatuses(statusUpdates));
            updated = true;
        }
        // Return the message status updates
        if(updated){
            // Fire the event on which the message should refresh message statuses from local.
            uiListener.onStatusesSync();
        }
    }

    // Returns all the updated message statuses for given message id and status type combos
    public List<MessageStatusChangeDTO> getUpdatedStatuses(Map<UUID, StatusType> messageStatusMap) {
        // If the opened chat has no messages no need to check for message status updates
        if(messageStatusMap.isEmpty()) {
            return null;
        }
        // Get the current message status in local database for all messages in the map
        List<MessageStatusChangeDTO> currentStatuses = databaseManager.executeQuery(() ->
                messageRepository.getStatusesForMessages(messageStatusMap.keySet()));
        // If no updates return null
        if(currentStatuses == null || currentStatuses.isEmpty()) {
            return null;
        }
        // If the updated status is different then the status provided to the method return the new status
        return currentStatuses.stream().filter(messageStatusChangeDTO ->
                !messageStatusChangeDTO.getStatus().equals(messageStatusMap.get(messageStatusChangeDTO.getMessageId()))).toList();
    }

    // Returns all the message statuses for a message from the local database
    public List<MessageStatusDisplay> getStatusesForMessage(UUID messageId) {
        return databaseManager.executeQuery(() -> messageRepository.getMessageStatuses(messageId));
    }

    // Use the id stored in the access token to get user id
    public UserDTO getUserFromToken(){
        return httpClient.findUserById(accessToken.get().getTokenUser(), accessToken.get().getTokenEncoded());
    }

    // Check if the chat was created and if the post response contains a location header, if it does get the newly
    // created chat from the specified location and return it to the user
    public ChatDTO createRegularChat(UserDTO user1, UserDTO user2) {
        // Create chat member DTOs from userDTOs so they can be added to chat
        ChatMemberDTO chosenUser = ChatMemberDTO.builder()
                .userId(user1.getId())
                .username(user1.getUsername())
                .role(ChatterRole.MEMBER)
                .mutedUntil(null)
                .pinned(false)
                .blocked(false)
                .build();

        ChatMemberDTO currentUser = ChatMemberDTO.builder()
                .userId(user2.getId())
                .username(user2.getUsername())
                .role(ChatterRole.MEMBER)
                .mutedUntil(null)
                .pinned(false)
                .blocked(false)
                .build();

        // Create chat DTO
        ChatDTO newChat = ChatDTO.builder()
                .chatType(ChatType.REGULAR)
                .users(List.of(chosenUser, currentUser))
                .build();

        // Check if the chat with these users already exist in the local database (can happen if chat had no messages)
        ChatDTO localChat = databaseManager.executeQuery(() -> chatRepository.findChatByUsers(user1.getId(), user2.getId()));
        if(localChat!=null) {
            return localChat;
        }

        // Create chat in remote database
        HttpResponse<String> response = httpClient.createChat(newChat, accessToken.get().getTokenEncoded());

        Optional<String> location = response.headers().firstValue("Location");

        // If creation was successful and location of the new chat is received get the new chat object
        if (location.isPresent() && response.statusCode() == 201) {
            // Save the chat into the local database before returning it to user
            ChatDTO createdChat = httpClient.findChat(location.get(), accessToken.get().getTokenEncoded());
            databaseManager.executeTransaction(() -> chatRepository.insertChat(createdChat));
            return createdChat;
        } else {
            return null;
        }
    }

    // Create group chat via POST request and save the new chat to local db
    public ChatDTO createGroupChat(String chatName, UserDTO creator, List<UserDTO> users) {
        // Create creator and users chat member objects
        List<ChatMemberDTO> chatMemberDTOS = new LinkedList<>();
        // First create chat member with creator role
        ChatMemberDTO creatorCm = ChatMemberDTO.builder()
                .userId(creator.getId())
                .username(creator.getUsername())
                .role(ChatterRole.CREATOR)
                .mutedUntil(null)
                .pinned(false)
                .blocked(false)
                .build();
        chatMemberDTOS.add(creatorCm);
        // Then create chat member objects for the regular members
        for(UserDTO userDTO : users){
            ChatMemberDTO chatMember = ChatMemberDTO.builder()
                    .userId(userDTO.getId())
                    .username(userDTO.getUsername())
                    .role(ChatterRole.MEMBER)
                    .mutedUntil(null)
                    .pinned(false)
                    .blocked(false)
                    .build();
            chatMemberDTOS.add(chatMember);
        }

        // Create chat DTO
        ChatDTO newChat = ChatDTO.builder()
                .chatName(chatName)
                .chatType(ChatType.GROUP)
                .users(chatMemberDTOS)
                .build();

        // Create chat in remote database
        HttpResponse<String> response = httpClient.createChat(newChat, accessToken.get().getTokenEncoded());

        Optional<String> location = response.headers().firstValue("Location");

        // If creation was successful and location of the new chat is received get the new chat object and also send the chat created event
        if (location.isPresent() && response.statusCode() == 201) {
            // Save the chat into the local database before returning it to user
            ChatDTO createdChat = httpClient.findChat(location.get(), accessToken.get().getTokenEncoded());
            databaseManager.executeTransaction(() -> chatRepository.insertChat(createdChat));

            // Send the chat created event through the socket
            socketClient.sendCreateGroup(createdChat);

            return createdChat;
        }

        return null;
    }

    // Login in case there is no refresh token
    public UserDTO login(AuthRequestDTO authRequestDTO) {

        // Perform login in http client
        TokenResponseDTO tokens = httpClient.login(authRequestDTO);
        if(tokens == null) {
            return null;
        }

        // Save access token in memory and save the refresh token to file
        try {
            accessToken.set(new JwtToken(tokens.getAccessToken()));
            tokenStorage.saveRefreshToken(tokens.getRefreshToken());
            hasRefreshToken = true;
        } catch (Exception e) {
            System.out.println("Error logging in, message: " + e.getMessage());
            return null;
        }

        return UserDTO.builder()
                .id(accessToken.get().getTokenUser())
                .username(authRequestDTO.getUsername())
                .build();
    }

    // Registration method hitting the register endpoint via the http client
    public int register(AuthRequestDTO authRequestDTO) {
        // Perform registration in http client
        HttpResponse<String> response = httpClient.register(authRequestDTO);
        return response.statusCode();
    }

    // Sets up the scheduling of the refreshAccessToken method when the user is logged in
    private void setupRefreshScheduling() {
        // Create a runnable
        Runnable refreshTask = this::refreshAccessToken;

        // Refresh every 14 minutes because the token lasts 15 minutes, run first execution 14 minutes after startup
        // if the refresh token is available (as we already run it for the first time in the constructor), we want to
        // refresh the access token because it has probably expired since last time the user started the app
        scheduler.scheduleAtFixedRate(refreshTask, 14, 14, TimeUnit.MINUTES);
    }

    // Get new access token before current access token expires
    private boolean refreshAccessToken(){
        try {
            String refreshToken = tokenStorage.loadRefreshToken();
            TokenResponseDTO tokens = httpClient.sendRefresh(refreshToken);
            accessToken.set(new JwtToken(tokens.getAccessToken()));
        } catch (Exception e) {
            System.out.println("Error getting access token, message: " + e.getMessage());
            return false;
        }
        return true;
    }

    // Handling logout logic
    public void logout() {
        // Removing the scheduler for jwt refresh
        scheduler.shutdownNow();

        // Set the last sync time for this user to the current moment, as we have up-to-date data until this point
        databaseManager.executeTransaction(() -> syncRepository.insertSync(Instant.now()));

        // Sending the logout message to the socket server so it can close to socket
        socketClient.sendLogoutMessage();
        performSocketCleanUp();

        // Sending logout request to revoke the refresh token
        HttpResponse<String> response = httpClient.logout(accessToken.get().getTokenEncoded());
        if (response.statusCode() == 204) {
            System.out.println("Logout successful");
        } else {
            System.out.println("Error logging out on the server side.");
        }

        // Deleting user token and keystore for the encrypted token file from the device
        File keystoreFile = new File(AppPaths.getAppDataDirectory().resolve(KEYSTORE_FILE).toString());
        File tokenFile = new File(AppPaths.getAppDataDirectory().resolve(TOKEN_FILE).toString());
        File passwordFile = new File(AppPaths.getAppDataDirectory().resolve(PASSWORD_FILE).toString());
        if (keystoreFile.delete() && tokenFile.delete() && passwordFile.delete()) {
            System.out.println("Deleted file: " + keystoreFile.getName());
            System.out.println("Deleted file: " + tokenFile.getName());
            System.out.println("Deleted file: " + passwordFile.getName());
        }
    }

    // Returns the result from findUserById repository method
    public UserDTO getUserById(UUID userId) {
        return databaseManager.executeQuery(() -> chatMemberRepository.findUserById(userId));
    }

}
