package org.chatapp.client.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.chatapp.client.interfaces.ChatClientListener;
import org.chatapp.client.interfaces.SidebarClickHandler;
import org.chatapp.client.interfaces.UiComponentListener;
import org.chatapp.client.service.ChatController;
import org.chatapp.client.service.DateConvertor;
import org.chatapp.client.service.OnlineUserTracker;
import org.chatapp.client.ui.components.*;
import org.chatapp.client.ui.helpers.MainFrameLayoutBuilder;
import org.chatapp.client.ui.records.MainFrameComponents;
import org.chatapp.client.ui.records.MessageStatusDisplay;
import org.chatapp.client.ui.records.Theme;
import org.chatapp.common.model.*;
import org.chatapp.common.model.socket.transport.SocketEventType;
import org.chatapp.common.model.socket.transport.UserStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainFrame extends JFrame implements ChatClientListener, UiComponentListener, SidebarClickHandler {

    // Current logged in user
    private final UserDTO activeUser;
    // We want to be able to get updated chat list from sync so this field is not final
    private List<ChatDTO> chatDTOList;
    // ChatController is the class through which all communication with server as well as local database is handled
    private final ChatController chatController;
    // Used for storing current status of users with which active user shares chats
    private final OnlineUserTracker onlineUserTracker;

    // Boolean flag that lets the constructor know if the data it received from controller was updated (new data was received from sync)
    private boolean receivedUpdate = false;
    // Boolean flag that lets the user know if constructor finished executing
    private boolean uiReady = false;
    // Boolean flag that shows if the chat create modal is currently open
    private boolean modalOpen = false;

    // UI components
    private MainFrameComponents uiComponents;
    private SidebarChatCard activeChat;

    // Declaring all colors to be used
    private Color chatBackgroundColor;
    private Color sidebarColor;
    private Color sidebarActiveColor;
    private Color navbarColor;

    public MainFrame(UserDTO activeUser, ChatController chatController) {
        this.activeUser = activeUser;
        this.chatController = chatController;
        chatController.socketClientSetup(activeUser, this);
        // This loads the color variables from the active theme
        updateColors();

        // This has to be after instantiating httpClient and objectMapper as we are using them to get the chats
        chatDTOList = chatController.loadChats(activeUser.getId());
        // Setting up the onlineUserTracker with the chatDTOList from the user
        this.onlineUserTracker = new OnlineUserTracker(chatDTOList);

        // Show a confirmation dialog when a user tries to exit
        handleAppExit(chatController);
        // Set the window size to full screen by default
        frameWindowSetup();

        // Building all child components
        uiComponents = setupChildComponents();

        // Check if new data was received from remote and update the sidebar if it was
        if(receivedUpdate) {
            uiReady = true;
            refreshOnSync();
        }
        uiReady = true;

        // Method call to chat controller which requests the snapshot here
        chatController.sendPresenceSnapRequest(onlineUserTracker.getUserIds());
    }

    // Creates all the child ui components and sets the active chat for the first time
    private MainFrameComponents setupChildComponents() {
        MainFrameLayoutBuilder mainFrameLayoutBuilder =
                new MainFrameLayoutBuilder(this, activeUser, chatDTOList, this::handleSendMessage);
        uiComponents = mainFrameLayoutBuilder.build();
        // Set active chat
        if(mainFrameLayoutBuilder.getFirstActiveChat() != null) {
            setActiveChat(mainFrameLayoutBuilder.getFirstActiveChat());
        }
        // Add the getImageAttachment executor reference to the message area panel class
        uiComponents.messageArea().setGetImageFromRemoteMethod(chatController);
        // If active chat is set, set it in child components as well
        if(activeChat != null) {
            uiComponents.messageArea().setActiveChat(activeChat);
            showChatMessages(activeChat.getChat(), true);
            uiComponents.messageScrollPane().setReachedEnd(false);
            activeChat.updateUnread(0);
        }
        setContentPane(uiComponents.contentPane());
        return uiComponents;
    }

    // Sets windows size and behavior
    private void frameWindowSetup() {
        setSize(1920, 1080);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // Opens a dialog so the user can confirm if they want to exit the program
    private void handleAppExit(ChatController chatController) {
        JFrame mainFrame = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int resp = JOptionPane.showConfirmDialog(mainFrame, "Are you sure you want to exit the program?",
                        "Exit program?", JOptionPane.YES_NO_OPTION);

                if (resp == JOptionPane.YES_OPTION) {
                    chatController.insertSync();
                    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                } else {
                    mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                }
            }
        });
    }

    // Refresh the sidebar by removing all chat sidebar cards and adding the new updated ones
    private void refreshSidebar() {
        SidebarChatCard active = uiComponents.sidebar().refreshSidebar(chatDTOList);
        setActiveChat(active);
        showChatMessages(activeChat.getChat(), false);
        activeChat.updateUnread(0);
    }

    // This method handles the user action of a trying to send a message
    private void handleSendMessage() {
        String messageContent = uiComponents.chatInputBox().getMessageTxt().getCleanText();
        ReplyMessagePanel reply = uiComponents.chatInputBox().getReplyPanelCopy();
        Path messageAttachment = uiComponents.chatInputBox().getMessageAttachment();
        // If sending the message fails do not add it to the message bubble
        if(messageAttachment == null) {
            if (!sendTextMessage()){
                // maybe add some error handling here
                return;
            }
        } else {
            String filename = sendFileMessage();
            if(filename == null) {
                return;
            }
            messageContent = filename;
        }

        // Add the message bubble to the chat main body
        addMessage(null, messageContent, Instant.now(), true, activeUser.getUsername(), null, messageAttachment, reply);
        uiComponents.chatInputBox().getMessageTxt().setText("");
    }

    // Method for handling sending the file message commands
    private String sendFileMessage() {
        String filename = null;
        // Determine weather a first message is being sent or just a regular message
        if(activeChat != null && activeChat.getChat().getLastMessageSentTime() == null && activeChat.getChat().getChatType().equals(ChatType.REGULAR)) {
            filename = chatController
                    .sendFirstMessageWithAttachment(uiComponents.chatInputBox().getMessageAttachment(), activeChat.getChat());
        } else {
            ReplyMessagePanel reply = uiComponents.chatInputBox().getReplyPanelCopy();
            filename = chatController
                    .sendMessageWithAttachment(uiComponents.chatInputBox().getMessageAttachment(),
                            activeChat.getChat(), (reply == null ? null : reply.getMessageId())
                    );
        }
        if(filename == null) {
            JOptionPane.showMessageDialog(this, "An error occurred while trying to send the message with attachment!",
                    "Error sending message", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if(!chatController.saveUserFile(filename, uiComponents.chatInputBox().getMessageAttachment())){
            JOptionPane.showMessageDialog(this, "An error occurred while trying to save the file to local storage!",
                    "Error saving file", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return filename;
    }

    // Method for running the send message command for text messages
    private boolean sendTextMessage() {
        // Checking if the message to be sent is the first message in the chat
        if(activeChat != null && activeChat.getChat().getLastMessageSentTime() == null && activeChat.getChat().getChatType().equals(ChatType.REGULAR)) {
            if(!chatController.sendFirstMessage(uiComponents.chatInputBox().getMessageTxt().getCleanText(), activeChat.getChat(), MessageType.CHAT_MESSAGE)){
                JOptionPane.showMessageDialog(this, "An error occurred while trying to send the first message for this chat!",
                        "Error sending first message", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else {
            // Send the message through the socket
            if(!sendMessage(uiComponents.chatInputBox().getMessageTxt().getCleanText())) {
                JOptionPane.showMessageDialog(this, "An error occurred while trying to send the message!",
                        "Error sending message", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    // This method handles the use action of logging out
    // Logout the user by deleting session data and returning them to the login screen
    private void handleLogout() {
        // Ask the user if he is sure he wants to log out
        int resp = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?",
                "Log out of your account?", JOptionPane.YES_NO_OPTION);

        // In case he does want to log out continue otherwise do nothing
        if (resp == JOptionPane.YES_OPTION) {
            // Deleting the user login data from the device (tokens, keys, etc...)
            chatController.logout();

            // Creating and opening new login window and deleting current window
            ThemeManager.setTheme("dark");
            FlatDarculaLaf.setup();
            JFrame loginFrame = new ChatClientGui();
            loginFrame.setVisible(true);
            this.dispose();
        }
    }

    // This method adds a message chat bubble to the message area
    public void addMessage(UUID messageId, String msgContent, Instant sentTime, boolean isSent,
                           String senderUsername, StatusType status, Path messageAttachment, ReplyMessagePanel reply) {
        MessageBubble bubble = uiComponents.messageArea().addMessage(
                messageId, msgContent, sentTime, isSent, senderUsername, status, false, messageAttachment, reply);

        MessageStatusChangeDTO pendingStatus = uiComponents.messageArea().getPendingStatuses().get(msgContent);
        if(pendingStatus != null) {
            if(uiComponents.messageArea().getPendingMessages().contains(bubble)) {
                uiComponents.messageArea().getPendingMessages().remove(bubble);
                uiComponents.messageArea().getMessageBubbles().put(pendingStatus.getMessageId(), bubble);
                bubble.setMessageId(pendingStatus.getMessageId());
                bubble.updateStatusIcon(pendingStatus.getStatus());
            }
        }
    }

    // This method gets all the messages for a chat from ChatController and displays them in the message area
    public void showChatMessages(ChatDTO chat, boolean syncStatuses) {
        // This means the chat is a newly created chat, and it doesn't have any new messages, and it
        // isn't even persisted in the database
        if (chat.getId() == null) {
            return;
        }
        List<MessageDTO> chatMessages;
        Map<UUID, MessageDTO> replyMap;
        try {
            chatMessages = chatController.getChatMessages(chat.getId(), 1, 25, syncStatuses);
            // Fetch all replies not in the message list
            replyMap = getReplyDtoMap(chatMessages);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Error loading chat messages", JOptionPane.ERROR_MESSAGE);
            return;
        }
        uiComponents.messageArea().showChatMessages(chat, chatMessages, replyMap);
        uiComponents.messageScrollPane().resetPageNumber();
        uiComponents.chatInputBox().clearInputField();

        // Notify the server that the user has opened the chat thus seen the messages in it, of course only if there are any messages to be seen
        if(!chatMessages.isEmpty()) {
            chatController.bulkUpdateSeenStatus(activeUser, activeChat.getChat().getId());
        }
    }

    // Sends message through message controller to be received by other users
    private boolean sendMessage(String messageContent) {
        // Get the reply from the input field and add the message id it to sendMessage arguments
        ReplyMessagePanel reply = uiComponents.chatInputBox().getReplyPanelCopy();
        return chatController.sendMessage(messageContent, activeChat.getChat(), MessageType.CHAT_MESSAGE,
                (reply == null ? null : reply.getMessageId()));
    }

    // Used for sending a new message status when user opens a message
    private boolean sendMessageSeenStatus(MessageDTO messageDTO) {
        messageDTO.setSender(activeUser);
        messageDTO.setContent(null);
        return chatController.sendStatusUpdateMessage(messageDTO);
    }

    public UserDTO getActiveUser() {
        return activeUser;
    }

    // This method opens a new chat between two users
    public void openNewChat(UserDTO userDTO) {
        // Create the new chat
        ChatDTO newChat = chatController.createRegularChat(activeUser, userDTO);

        // Add chat to the sidebar
        SidebarChatCard newChatCard = uiComponents.sidebar().addChatToSidebar(newChat);
        chatDTOList.add(newChat);

        // Add the user in the new chat to online user tracker and send presence snap request if they aren't in it
        if(!onlineUserTracker.checkIfUserStatusAvailable(userDTO.getId())){
            onlineUserTracker.addNewUser(userDTO.getId());
            chatController.sendPresenceSnapRequest(Set.of(userDTO.getId()));
        }

        // Make the chat selected
        setActiveChat(newChatCard);
        showChatMessages(newChat, true);

        // Clear the chat area because the new chat can't have any messages
        uiComponents.sidebar().revalidate();
        uiComponents.sidebar().repaint();

        uiComponents.messageArea().removeAll();
        uiComponents.messageArea().revalidate();
        uiComponents.messageArea().repaint();
    }

    // This method creates and opens a group chat between the selected users
    public void openNewGroupChat(String chatName, List<UserDTO> users){
        // Create the new chat
        ChatDTO newChat = chatController.createGroupChat(chatName, activeUser, users);

        if(newChat == null) {
            JOptionPane.showMessageDialog(this, "An error occurred while trying to create group chat!",
                    "Error creating group chat", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Add chat to the sidebar
        SidebarChatCard newChatCard = uiComponents.sidebar().addChatToSidebar(newChat);
        chatDTOList.add(newChat);

        // Add each new user in the new chat to online user tracker if they aren't in it
        for(UserDTO userDTO : users) {
            if(!onlineUserTracker.checkIfUserStatusAvailable(userDTO.getId())){
                onlineUserTracker.addNewUser(userDTO.getId());
            }
        }

        // Make the chat selected
        setActiveChat(newChatCard);

        // Clear the chat area because the new chat can't have any messages
        uiComponents.sidebar().revalidate();
        uiComponents.sidebar().repaint();

        uiComponents.messageArea().removeAll();
        uiComponents.messageArea().revalidate();
        uiComponents.messageArea().repaint();
    }

    // This calls the search box method responsible for displaying the search results, it also removes the active user
    // from the search results
    public void showSearchResults(List<UserDTO> results) {
        if(modalOpen){
            uiComponents.searchBox().getModal().updateSearchResults(results.stream().filter(userDTO -> !userDTO.getId().equals(activeUser.getId())).toList(), false);
        }else {
            // Getting all chat names as shown in the sidebar
            List<String> chatNames = chatDTOList.stream()
                    .filter(chatDTO -> uiComponents.sidebar().getChatCard(chatDTO.getId())!=null)
                    .map(chatDTO -> chatDTO.getChatName(activeUser)).toList();

            // Removing active user and users with which the current user has a regular chat displayed in the
            // sidebar before passing on the list of users to be displayed to the search box results
            uiComponents.searchBox().updateSearchResults(results.stream().filter(userDTO -> !userDTO.getId().equals(activeUser.getId())
                    && !chatNames.contains(userDTO.getUsername())).toList());
        }
    }

    // Get all the users with whom the current user has contact with (mutual chats)
    public Set<UserDTO> getMutualUsers(){
        return  chatDTOList.stream().flatMap(chatDTO -> chatDTO
                .getUsers()
                .stream()
                // Make sure the user does not receive himself
                .filter(chatMemberDTO -> !chatMemberDTO.getUserId().equals(activeUser.getId()))
                .map(chatMemberDTO -> UserDTO.builder()
                        .id(chatMemberDTO.getUserId())
                        .username(chatMemberDTO.getUsername())
                        .build())).collect(Collectors.toSet());
    }

    // This calls the chatController method which sends the search query to the server
    public void sendSearchQuery(String query) {
        if(!chatController.sendSearchQuery(query)){
            JOptionPane.showMessageDialog(this, "An error occurred while trying search users!",
                    "Search error", JOptionPane.ERROR_MESSAGE);

        }
    }

    // A method that loads new data from remote sync and refreshes the ui accordingly
    private void refreshOnSync(){
        // Update the chat dto list and refresh the sidebar
        chatDTOList = chatController.getChatsLocal(activeUser.getId());
        refreshSidebar();

        // Check if the new chat members exist in the online user tracker and add their id's to a UUID list, if the
        // list is not empty at the end send new snapshot request for them.
        Set<UUID> newUsers = onlineUserTracker.updateUserSet(chatDTOList);
        if(!newUsers.isEmpty()){
            chatController.sendPresenceSnapRequest(newUsers);
        }
    }

    @Override
    public void onMessageReceived(MessageDTO message) {
        // Display message logic
        SwingUtilities.invokeLater(() ->{
                    if(activeChat!=null && activeChat.getChat().getId().equals(message.getChatId())){
                        ReplyMessagePanel reply = null;
                        // Check if message has reply id
                        if(message.getReplyToId() != null) {
                            MessageDTO replyMessage = chatController.findMessageById(message.getReplyToId());

                            // Initialize reply panel to be placed in message bubble
                            if(replyMessage != null) {
                                reply = new ReplyMessagePanel(replyMessage.getId(),
                                        replyMessage.getSender().getUsername(),
                                        DateConvertor.toHourMinString(replyMessage.getSentTime()),
                                        replyMessage.getContent(),
                                        // If message is file message provide the path to the message's image
                                        replyMessage.getMessageType().equals(MessageType.FILE_MESSAGE) ?
                                                ChatClientGui.LOCAL_FILES_PATH.resolve("images").resolve(replyMessage.getContent()) :
                                                null
                                );
                            } else {
                                System.out.println("Message reply not found starting background task!");
                                // If message is not found in local fetch the message from remote db in the background as a CompletableFuture
                                chatController.getReplyFromRemote(message);
                                reply = new ReplyMessagePanel(message.getReplyToId(),
                                        null,
                                        null,
                                        "Loading reply...",
                                        null
                                );
                            }

                            reply.getRemoveButton().setVisible(false);
                        }

                        // Get message attachment if message is a file message
                        Path messageAttachment = null;
                        if(message.getMessageType() == MessageType.FILE_MESSAGE) {
                            messageAttachment = ChatClientGui.LOCAL_FILES_PATH.resolve("images").resolve(message.getContent());
                        }
                        // Add message to message area
                        addMessage(message.getId(), message.getContent(), message.getSentTime(), false,
                                    message.getSender().getUsername(), null, messageAttachment, reply);

                        // Update the chat card in the sidebar
                        activeChat.updateLastMessage(message);
                        // Send the message SEEN status update for the message received in active chat
                        sendMessageSeenStatus(message);
                        // Update that info to the local database as well
                        chatController.updateChatData(activeChat.getChat());
                    } else {
                        // Update the chat card in the sidebar
                        SidebarChatCard chatCard = uiComponents.sidebar().getChatCard(message.getChatId());
                        // Check if the chat for the message is in the sidebar, if it isn't add it to the sidebar first
                        if(chatCard == null){
                            chatCard = handleMessageInNewChat(message);
                        }
                        chatCard.incrementUnread();
                        chatCard.updateLastMessage(message);
                        // Update that info to the local database as well
                        chatController.updateChatData(chatCard.getChat());
                        // Check if message has reply id
                        if(message.getReplyToId() != null) {
                            MessageDTO replyMessage = chatController.findMessageById(message.getReplyToId());
                            // Initialize reply panel to be placed in message bubble
                            if(replyMessage == null) {
                                System.out.println("Message reply not found starting background task!");
                                // If message is not found in local fetch the message from remote db in the background as a CompletableFuture
                                chatController.getReplyFromRemote(message);
                            }
                        }
                    }

                    // Refresh sidebar if message is received to display the chat with the new message at the top of the sidebar
                    // Skip this if message is in a chat that is already top of the list
                    SidebarChatCard firstChatCard = uiComponents.sidebar().getFirstChat();
                    if(firstChatCard != null &&
                            !firstChatCard.getChat().getId().equals(message.getChatId())) {
                        UUID activeChatId = activeChat.getChat().getId();
                        uiComponents.sidebar().refreshSidebar(chatDTOList);
                        setActiveChat(uiComponents.sidebar().getChatCard(activeChatId));
                    }
                }
        );
    }

    // Creates chatCard, adds it to the sidebar and adds message sender to onlineUserTracker if not there already
    private SidebarChatCard handleMessageInNewChat(MessageDTO message) {
        SidebarChatCard chatCard;
        // Find the chat in local database and add it to the sidebar
        ChatDTO chat = chatController.findChatById(message.getChatId());
        chatCard = uiComponents.sidebar().addChatToSidebar(chat);
        chatDTOList.add(chat);

        // Adding the user to the status tracker if not in it already and set them as online as they
        // must be online if they just sent a message
        if(!onlineUserTracker.checkIfUserStatusAvailable(message.getSender().getId())){
            onlineUserTracker.addNewUser(message.getSender().getId());
            onlineUserTracker.setUserStatus(message.getSender().getId(), true);
        }
        return chatCard;
    }

    @Override
    public void onSearchResultReceived(List<UserDTO> result) {
        // Display search results
        SwingUtilities.invokeLater(() ->
                showSearchResults(result)
        );
    }

    @Override
    public void onChatterStatusChange(UUID userId, UserStatus userStatus) {
        // Check if the change is in the active chat and if it is redisplay the chat status
        SwingUtilities.invokeLater(() -> {
            // Set user status using our method so we can stop using the toggle
            onlineUserTracker.setUserStatus(userId, userStatus.equals(UserStatus.CONNECTED));
            if(activeChat != null && activeChat.getChat().getUsers().stream().map(ChatMemberDTO::getUserId).toList().contains(userId)){
                setActiveChatNameLabel();
            }
        });
    }

    @Override
    public void onPresenceSnapshotReceived(List<UUID> onlineUserIds) {
        SwingUtilities.invokeLater(() -> {
            onlineUserIds.forEach(uuid -> onlineUserTracker.setUserStatus(uuid, true));
            // Check if any of the active chat users has changed status, if it has refresh label
            if(activeChat.getChat().getUsers()
                    .stream()
                    .map(ChatMemberDTO::getUserId)
                    .anyMatch(onlineUserIds::contains)){
                setActiveChatNameLabel();
            }
        });
    }

    // Handles the message status change and updates the status icon in the message bubbles
    @Override
    public void onSingleMessageStatusChange(MessageDTO message, SocketEventType messageStatus) {
        SwingUtilities.invokeLater(() -> {
            if(messageStatus.equals(SocketEventType.SENT_MESSAGE)) {
                // If message status is changed to sent we need to find the message in the pending messages list and update
                // its status icon and move it to the message bubbles map
                MessageBubble bubbleToChange = uiComponents.messageArea().getPendingMessages().stream()
                        .filter(messageBubble ->
                                messageBubble.getBubbleContent().equals(message.getContent())).findFirst().orElse(null);

                // Update status icon for the message to sent
                if(bubbleToChange != null ) {
                    uiComponents.messageArea().getPendingMessages().remove(bubbleToChange);
                    uiComponents.messageArea().getMessageBubbles().put(message.getId(), bubbleToChange);
                    bubbleToChange.updateStatusIcon(StatusType.SENT);
                    bubbleToChange.setMessageId(message.getId());
                } else {
                    uiComponents.messageArea().getPendingStatuses().put(
                            message.getContent(),
                            MessageStatusChangeDTO.builder()
                                    .status(StatusType.SENT)
                                    .messageId(message.getId())
                                    .build());
                }

                // Update the sidebar chat card to show the message user sent as last message
                activeChat.updateLastMessage(message);
                // Update that info to the local database as well
                chatController.updateChatData(activeChat.getChat());
            } else if(messageStatus.equals(SocketEventType.DELIVERED_MESSAGE)) {
                updateStatusIfActiveChatAndUser(message, StatusType.DELIVERED);
            } else{
                updateStatusIfActiveChatAndUser(message, StatusType.SEEN);
            }
        });
    }

    // Updates the status of the message if it was sent in the active chat by the active user
    private void updateStatusIfActiveChatAndUser(MessageDTO message, StatusType statusType){
        if(!activeChat.getChat().getId().equals(message.getChatId())){
            return;
        }
        MessageBubble bubbleToChange = uiComponents.messageArea().getMessageBubbles().get(message.getId());
        // If message is still not loaded add the status to pending statuses
        if(bubbleToChange == null) {
            String messageContent = message.getContent();
            // Need to find the pending status for this message. The content is null because we don't send the message
            // content with status updates
            for(String key : uiComponents.messageArea().getPendingStatuses().keySet()) {
                MessageStatusChangeDTO changeDto = uiComponents.messageArea().getPendingStatuses().get(key);
                if(changeDto.getMessageId().equals(message.getId())) {
                        messageContent = key;
                        break;
                }
            }
            // Insert new status change into the pending statuses map with the updated status
            uiComponents.messageArea().getPendingStatuses().put(
                    messageContent,
                    MessageStatusChangeDTO.builder()
                            .status(statusType)
                            .messageId(message.getId())
                            .build());
            return;
        }

        if(!bubbleToChange.getSender().equals(activeUser.getUsername())){
            return;
        }
        bubbleToChange.updateStatusIcon(statusType);
    }


    // Event handler for when the sync chats sync event is received(new data received from the database about chats and recent messages)
    @Override
    public void onChatsSync() {
        receivedUpdate = true;
        if(uiReady){
            SwingUtilities.invokeLater(this::refreshOnSync);
        }
    }

    // Event handler for when the sync statuses event is received(new data received from the remote database about message statuses for given chat)
    @Override
    public void onStatusesSync() {
        // Takes all messages currently in the message area and updates them if their statuses were changed
        SwingUtilities.invokeLater(() ->
                uiComponents.messageArea().updateBubbleStatuses(chatController.getUpdatedStatuses(uiComponents.messageArea().getForRefreshStatus())));
    }

    @Override
    public void onBulkSeenStatusChange(UUID chatId) {
        // Set status icon to seen for all active-user sent messages
        if(activeChat!= null && chatId.equals(activeChat.getChat().getId())) {
            // Iterate through the messages and set the status to seen for each message sent by the active user
            for(UUID key : uiComponents.messageArea().getMessageBubbles().keySet()) {
                if(uiComponents.messageArea().getMessageBubbles().get(key).getSender().equals(activeUser.getUsername())){
                    uiComponents.messageArea().getMessageBubbles().get(key).updateStatusIcon(StatusType.SEEN);
                }
            }
        }
    }

    // This event handles the bulk delivered status change in the ui
    @Override
    public void onBulkDeliveredStatusChange(List<UUID> messageList) {
        // Set status icon to delivered for all messages in the message list which are found in active chat
        // Iterate through the messages and set the status to delivered for each message found in the message list
        for(UUID messageId : messageList) {
            MessageBubble messageBubble = uiComponents.messageArea().getMessageBubbles().get(messageId);
            if (messageBubble != null && messageBubble.getSender().equals(activeUser.getUsername())) {
                messageBubble.updateStatusIcon(StatusType.DELIVERED);
            }
        }
    }

    // This event handles the newly created group chat
    @Override
    public void onGroupCreated(ChatDTO groupChat) {
        // Add the chat card to the sidebar
        uiComponents.sidebar().addChatToSidebar(groupChat);
        chatDTOList.add(groupChat);

        // Set of new users
        Set<UUID> newUsers = new HashSet<>();

        // Iterate through chat
        for(ChatMemberDTO chatMember : groupChat.getUsers()){
            // Adding the user to the status tracker if not in it already
            if(!onlineUserTracker.checkIfUserStatusAvailable(chatMember.getUserId())
                    && !chatMember.getUserId().equals(activeUser.getId())) {
                onlineUserTracker.addNewUser(chatMember.getUserId());
                newUsers.add(chatMember.getUserId());
            }
        }

        // Send a presence snapshot request for all the new users
        if(!newUsers.isEmpty()){
            chatController.sendPresenceSnapRequest(newUsers);
        }
    }

    @Override
    public void onDeleteMessage(MessageDTO message) {
        // Check if the message is in active chat, if so update it's content in the message bubble
        if(activeChat!=null && activeChat.getChat().getId().equals(message.getChatId())){
            MessageBubble bubble = uiComponents.messageArea().getMessageBubbles().get(message.getId());
            if(bubble!=null) {
                bubble.eraseBubbleContent();
            }
            // Check if any of the message bubbles replied to the delete message, if they have update the content of
            // the reply message panel to "Message deleted"
            for(MessageBubble messageBubble : uiComponents.messageArea().getMessageBubbles().values()) {
                // If reply panel's message id matches id of the given message update it's content
                if(messageBubble.getReplyPanel() != null && messageBubble.getReplyPanel().getMessageId().equals(message.getId())) {
                    messageBubble.updateReplyContentOnDelete();
                }
            }
        }

        // Fetch the sidebar card for the chat in which the message was deleted
        SidebarChatCard chatCard = uiComponents.sidebar()
                .getChatCard(message.getChatId());
        // Check if the deleted message was the last message of the chat in question and if so update the sidebar card
        if(chatCard.getChat().getLastMessageSentTime().equals(message.getSentTime()) &&
                chatCard.getChat().getLastMessageContent().equals(message.getContent())){
            chatCard.handleLastMessageDeleted();
            chatController.updateChatData(chatCard.getChat());
        }
    }

    @Override
    public void onReplyLoaded(MessageDTO messageDTO) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Handling the on reply loaded event");
            findAndRefreshReplyPanel(messageDTO);
        });
    }

    // This method goes through message bubbles and refreshes the data in reply panel with same id as given messageDTO
    private void findAndRefreshReplyPanel(MessageDTO messageDTO) {
        // If the reply loaded event is received for a message which is not in the active chat don't do anything
        // (This can happen if we switch chats while replies are loading)
        if(!messageDTO.getChatId().equals(activeChat.getChat().getId())) {
            return;
        }
        for(int i = 0; i< uiComponents.messageArea().getComponents().length; i++) {
            // Check if component is a JPanel
            if(!(uiComponents.messageArea().getComponent(i) instanceof JPanel wrapper)) {
                continue;
            }

            // Check if the JPanel holds a MessageBubble
            if(!(wrapper.getComponent(0) instanceof MessageBubble bubble)) {
                continue;
            }

            // Check if the reply panel's id in the message bubble matches the messageDTO ID
            if(bubble.getReplyPanel() != null &&
                    bubble.getReplyPanel().getMessageId().equals(messageDTO.getId())) {
                // If ids match update the reply panel inside the message bubble
                ReplyMessagePanel reply = new ReplyMessagePanel(messageDTO.getId(),
                        messageDTO.getSender().getUsername(),
                        DateConvertor.toHourMinString(messageDTO.getSentTime()),
                        messageDTO.getContent(),
                        // If message is file message provide the path to the message's image
                        messageDTO.getMessageType().equals(MessageType.FILE_MESSAGE) ?
                                ChatClientGui.LOCAL_FILES_PATH.resolve("images").resolve(messageDTO.getContent()) :
                                null
                );
                bubble.updateReplyPanel(reply);
                // Update the reply info panel above the reply as well
                if(uiComponents.messageArea().getComponent(i-1) instanceof ReplyInfoPanel replyInfoPanel) {
                    replyInfoPanel.setOriginalPoster(messageDTO.getSender().getUsername());
                }
            }
        }
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            // Creating and opening new login window and deleting current window
            JFrame loginFrame = new ChatClientGui();
            loginFrame.setVisible(true);
            this.dispose();

            JOptionPane.showMessageDialog(loginFrame, "Server disconnected!", "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    @Override
    public void onServerError(String error) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    // UI COMPONENT EVENTS HANDLING
    @Override
    public void onScrollToBottom() {

    }

    // Could trying use SwingUtilities.invokeLater() here btw
    @Override
    public void onScrollToTop(int pageNumber, int pageSize) {
        // Check if active chat is null
        if(activeChat == null) {
            return;
        }

        // Get a page of messages for the active chat
        List<MessageDTO> messages;
        try {
            messages = chatController.getChatMessages(activeChat.getChat().getId(), pageNumber, pageSize, true);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Error loading previous chat messages", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // If the message list is not null return refresh the message area with the new messages
        if(messages!= null && !messages.isEmpty()) {
            Map<UUID, MessageDTO> replyMap = getReplyDtoMap(messages);
            uiComponents.messageArea().refreshChatMessages(messages, replyMap);
        } else {
            // Notify the scroll pane object that there are no more messages to be loaded from the active chat
            uiComponents.messageScrollPane().setReachedEnd(true);
        }
    }

    // Gets all replyIds from messageDTOs in the list and fetches all needed replies form the local database
    private Map<UUID, MessageDTO> getReplyDtoMap(List<MessageDTO> messages) {
        List<MessageDTO> replyDTOs = chatController.getLocalMessagesWhereIdIn(messages.stream()
                .map(MessageDTO::getReplyToId)
                .filter(Objects::nonNull).toList());
        return replyDTOs.stream()
                .collect(Collectors.toMap(
                        MessageDTO::getId,
                        messageDTO -> messageDTO,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public void onLogoutEvent() {
        SwingUtilities.invokeLater(this::handleLogout);
    }

    // This event handles the message bubble right click popup menu actions
    @Override
    public void onMessageMenuEvent(String event, MessageBubble messageBubble) {
        UUID messageID = null;
        // Get the message id from the message bubble map in messageArea by comparing map values to received bubble
        for(UUID key : uiComponents.messageArea().getMessageBubbles().keySet()) {
            if(uiComponents.messageArea().getMessageBubbles().get(key).equals(messageBubble)){
                messageID = key;
            }
        }
        // Check which event type was received and perform the appropriate action
        switch (event) {
            case "recipients" -> {
                List<MessageStatusDisplay> messageStatuses = chatController.getStatusesForMessage(messageID);
                SeenByModal userStatusesModal = new SeenByModal(this, messageStatuses);
                userStatusesModal.setVisible(true);
            }
            case "copy" -> {
                // Get the message text and wrap it in selection body
                String messageText = messageBubble.getBubbleContent();
                StringSelection textSelected = new StringSelection(messageText);

                // Get the system clipboard and set the contents
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(textSelected, null);
            }
            case "delete" -> {
                // Send delete message event
                chatController.deleteMessage(messageID);
            }
            case "reply" -> {
                if(messageBubble.getBubbleContent() == null) {
                    JOptionPane.showMessageDialog(this, "You cannot reply to deleted messages!",
                            "Replying to deleted", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                uiComponents.chatInputBox().handleReplyPicked(messageBubble);
            }
            case "view-image" -> {
                new ImageViewer(messageBubble.getMessageAttachmentPath()).setVisible(true);
            }
            default -> System.out.println("Event still not handled -> " + event);
        }
    }

    // Changes flat laf look and feel theme and also applies new custom color theme to child components
    @Override
    public void onThemeChange(String themeName) {
        // If the new theme name is light set color theme to light otherwise set it to dark
        if(themeName.equals("light")) {
            ThemeManager.setTheme("light");
            FlatLightLaf.setup();
        } else {
            ThemeManager.setTheme("dark");
            FlatDarculaLaf.setup();
        }

        // Update colors
        updateColors();
        // Change colors for mainframe object
        uiComponents.body().setBackground(chatBackgroundColor);
        uiComponents.left().setBackground(navbarColor);
        uiComponents.sidebarContainer().getViewport().setBackground(sidebarColor);

        // Apply custom color theme to all children implementing the ThemedComponent interface
        ThemeUtil.applyThemeToTree(this);
        // Paint the active chat sidebar panel
        if(activeChat != null) {
            activeChat.changeBackgroundColor(sidebarActiveColor);
        }

        // Repaint and revalidate mainframe and child components
        SwingUtilities.updateComponentTreeUI(this);
        revalidate();
        repaint();
    }

    // Sets new active chat card to have a different color, and reverts color of the old active chat back to default
    public void setActiveChat(SidebarChatCard activeChat) {
        this.activeChat = activeChat;
        uiComponents.sidebar().setActiveChat(activeChat);
        if(uiComponents.messageArea()!=null) {
            uiComponents.messageArea().setActiveChat(activeChat);
            uiComponents.messageScrollPane().setReachedEnd(false);
        }
        // If the chat is a chat with messages updating unread count, otherwise skipp this step
        if(activeChat.getChat().getLastMessageSentTime() != null){
            // Update the unread message count to 0 in the local database
            chatController.resetUnreadCount(activeChat.getChat());
        }

        setActiveChatNameLabel();
    }

    // Fills the active chat name label with the chat name and also the status of the chat(online/offline)
    public void setActiveChatNameLabel(){
        uiComponents.navbar().setActiveChatNameLabel(activeChat, onlineUserTracker, activeUser);
    }

    // Sets the modal open value to the desired value
    public void setModalOpen(boolean modalOpen) {
        this.modalOpen = modalOpen;
    }

    // Updates the color fields based on the values in the active theme file
    private void updateColors(){
        Theme theme = ThemeManager.getTheme();
        chatBackgroundColor = theme.primaryColor();
        sidebarColor = theme.sidebarColor();
        sidebarActiveColor = theme.sidebarActiveColor();
        navbarColor = theme.navbarColor();
    }

    @Override
    public void handleSidebarClick(SidebarChatCard card) {
        setActiveChat(card);
        showChatMessages(card.getChat(), true);
    }
}