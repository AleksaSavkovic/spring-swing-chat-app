package org.chatapp.client.ui.components;

import org.chatapp.client.interfaces.GetMessageAttachmentExecutor;
import org.chatapp.client.service.DateConvertor;
import org.chatapp.client.ui.ChatClientGui;
import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.client.interfaces.UiComponentListener;
import org.chatapp.client.ui.helpers.ImageUtils;
import org.chatapp.client.ui.records.Theme;
import org.chatapp.common.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageAreaPanel extends JPanel implements ThemedComponent {

    private UiComponentListener parentObject;
    private SidebarChatCard activeChat;
    private Map<UUID, MessageBubble> messageBubbles;
    private Map<String, MessageStatusChangeDTO> pendingStatuses;

    private GetMessageAttachmentExecutor getImageFromRemote;
    private List<MessageBubble> pendingMessages;
    private final UserDTO activeUser;
    private final ExecutorService imageExecutor =
            Executors.newFixedThreadPool(4);

    public MessageAreaPanel(UserDTO activeUser) {
        super();
        this.activeUser = activeUser;
        setBackground(ThemeManager.getTheme().primaryColor());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    // This method adds a message chat bubble to the message area
    public MessageBubble addMessage(UUID messageId, String msgContent, Instant sentTime, boolean isSent, String senderUsername,
                           StatusType status, boolean inverted, Path messageAttachment, ReplyMessagePanel reply) {
        // Getting chat type in order to see whether to add username to the message bubbles
        ChatType chatType = activeChat.getChat().getChatType();

        // Create the message bubble
        MessageBubble bubble = new MessageBubble(msgContent, sentTime, isSent, chatType.equals(ChatType.GROUP),
                senderUsername, status, parentObject, messageAttachment, reply);


        // If message has an attachment get the image and scaled it down to bubble size asynchronously as a background task
        if(messageAttachment != null){
            CompletableFuture.supplyAsync(()-> {
                try {
                    return ImageUtils.getScaledDownImage(readImageFromPath(messageAttachment, messageId),
                            MessageBubble.MAX_IMAGE_DIMENSION,
                            MessageBubble.MIN_IMAGE_DIMENSION);
                } catch (Exception e) {
                    System.out.println("Error reading image from file: " + e.getMessage());
                    return null;
                }
                }, imageExecutor).thenAccept(image ->
                    SwingUtilities.invokeLater(() -> bubble.setImageLabelImage(image))
                );
        }

        // If messageId is not null put it in the messageBubbles map, otherwise put it into pendingMessages list
        if(messageId != null) {
            messageBubbles.put(messageId, bubble);
            bubble.setMessageId(messageId);
        } else {
            pendingMessages.add(bubble);
        }


        // If message is a reply message add the reply panel before or after inserting the message
        // based on if the insert order is inverted
        if(reply!=null && !inverted) {
            addReplyInfoPanel(senderUsername, reply.getReplySender(), isSent, inverted);
        }
        addMessageToPanel(bubble, inverted);
        if(reply!=null && inverted) {
            addReplyInfoPanel(senderUsername, reply.getReplySender(), isSent, inverted);
        }

        if(!inverted) {
            // Auto-scroll to bottom
            SwingUtilities.invokeLater(parentObject::onScrollToBottom);
        }

        return bubble;
    }

    // Get the image from the file path
    public BufferedImage readImageFromPath(Path path, UUID messageId) {
        // If file does not exist and message id is not null try getting the file from remote and saving it to local folder
        if(messageId != null && !Files.exists(path)) {
            getImageFromRemote.getMessageAttachment(messageId);
        }
        try {
            return ImageUtils.readThumbnailFromLocalPath(path, (int)MessageBubble.MAX_IMAGE_DIMENSION.getHeight());
        } catch (IOException e) {
            System.out.println("Error loading image from file path");
            return null;
        }
    }

    // This method adds an already created chat message bubble to the message area
    private void addMessageToPanel(MessageBubble bubble, boolean inverted) {
        // Create wrapper box that handles the message bubble positioning in the message area
        JPanel wrapper = new JPanel(new FlowLayout(bubble.isSent() ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 5));
        wrapper.setOpaque(false);

        // Add bubble to wrapper and set wrapper dimensions
        wrapper.add(bubble);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int)bubble.getPreferredSize().getHeight() + 20));
        wrapper.setPreferredSize(new Dimension((int)wrapper.getPreferredSize().getWidth(), (int)bubble.getPreferredSize().getHeight() + 20));

        // Add the wrapper to the message area
        if(inverted) {
            add(wrapper,0);
        }else{
            add(wrapper);
        }
        revalidate();
        repaint();
    }

    // Load the messages to message area upon opening a chat
    public void showChatMessages(ChatDTO chat, List<MessageDTO> chatMessages, Map<UUID, MessageDTO> replies) {
        messageBubbles = new HashMap<>();
        pendingMessages = new ArrayList<>();
        pendingStatuses = new HashMap<>();
        removeAll();
        revalidate();
        repaint();

        // This means the chat is a newly created chat, and it doesn't have any new messages, and it
        // isn't even persisted in the database
        if (chat.getId() == null) {
            return;
        }

        if(!chatMessages.isEmpty() &&
                !DateConvertor.toLocal(chatMessages.get(0).getSentTime()).equals(LocalDate.now())){
            addDateBubble(LocalDate.now());
        }

        // Go through chat messages and display them in message bubbles with dates in between chat sessions on different dates
        createAndAddMessageBubbles(chatMessages, replies);

        // Auto-scroll to bottom
        SwingUtilities.invokeLater(parentObject::onScrollToBottom);
    }

    // Add a new portion of messages to the message area
    public void refreshChatMessages(List<MessageDTO> chatMessages, Map<UUID, MessageDTO> replies) {
        // DatePanel object is always the first in message area before loading new messages
        DatePanel oldestDate = (DatePanel)getComponent(0);
        // We remove the first element of the chat because that is the date of the first message in the message area,
        // if there are more messages of the same date in the new page
        if(oldestDate.getDate().equals(DateConvertor.toLocal(chatMessages.get(0).getSentTime()))) {
            remove(0);
        }
        revalidate();
        repaint();

        // Save the viewport position before loading older messages so we can scroll down to newest of the added messages
        JViewport viewport = (JViewport)this.getParent();
        Point viewPositionBefore = viewport.getViewPosition();
        int heightBefore = getHeight();

        // Go through chat messages and display them in message bubbles with dates in between chat sessions on different dates
        createAndAddMessageBubbles(chatMessages, replies);

        // Scroll to the newest added message after rendering all the messages in the chat
        SwingUtilities.invokeLater(() -> {
            // Get the position of the newest message from the older added messages
            int heightAfter = getHeight();
            int heightDiff = heightAfter - heightBefore;

            viewport.setViewPosition(new Point(
                    viewPositionBefore.x,
                    viewPositionBefore.y + heightDiff
            ));
        });
    }

    // For a list of messages creates message bubbles for them and adds them to the message area
    private void createAndAddMessageBubbles(List<MessageDTO> chatMessages, Map<UUID, MessageDTO> replies) {
        for(int i = 0; i< chatMessages.size(); i++) {
            // Default status type is SENT otherwise
            StatusType status = StatusType.SENT;

            // If the message was sent by current user we need to show status icon of the message
            boolean activeUserMessage = chatMessages.get(i).getSender().getId().equals(activeUser.getId());

            // Checking the message status of a message if it was sent by active user
            if(activeUserMessage){
                for(MessageStatusDTO messageStatus : chatMessages.get(i).getStatuses()){
                    // If status type for any user is seen mark the message as seen and stop iterating
                    if(messageStatus.getStatus().equals(StatusType.SEEN)) {
                        status = StatusType.SEEN;
                        break;
                    } else if(messageStatus.getStatus().equals(StatusType.DELIVERED)) {
                        status = StatusType.DELIVERED;
                    }
                }
            }
            Path messageAttachment = null;
            // If message is of type file message it will hold the name of the save file as content
            if(chatMessages.get(i).getMessageType().equals(MessageType.FILE_MESSAGE) && chatMessages.get(i).getContent() != null) {
                messageAttachment = ChatClientGui.LOCAL_FILES_PATH.resolve("images").resolve(chatMessages.get(i).getContent());
            }

            // Add message constructor
            addMessage(chatMessages.get(i).getId(),
                    chatMessages.get(i).getContent(),
                    chatMessages.get(i).getSentTime(),
                    activeUserMessage,
                    chatMessages.get(i).getSender().getUsername(),
                    activeUserMessage ? status : null,
                    true,
                    messageAttachment,
                    (chatMessages.get(i).getReplyToId() == null || chatMessages.get(i).getContent() == null) ?
                            null :
                            createReplyPanelFromMessageDTO(replies.get(chatMessages.get(i).getReplyToId()))
            );

            // Add date bubble between two messages if they are sent on different dates
            if(i == chatMessages.size()-1 ||
                    !DateConvertor.toLocal(chatMessages.get(i).getSentTime())
                            .equals(DateConvertor.toLocal(chatMessages.get(i+1).getSentTime()))){
                addDateBubble(DateConvertor.toLocal(chatMessages.get(i).getSentTime()));
            }
        }
    }

    // This method adds a date in the middle of the chat whenever a message is sent in a new day
    public void addDateBubble(LocalDate date) {
        // Initialize custom date wrapper for display in chat
        DatePanel datePanel = new DatePanel(date);

        // Add the wrapper to the message area
        add(datePanel, 0);
        revalidate();
        repaint();
    }

    // This method adds reply info above the reply bubbles
    public void addReplyInfoPanel(String replier, String originalPoster, boolean isSent, boolean inverted) {
        if(originalPoster==null) {
            originalPoster = "?";
        }
        ReplyInfoPanel replyInfo = new ReplyInfoPanel(
                replier.equals(activeUser.getUsername()) ? "You" : replier,
                originalPoster.equals(activeUser.getUsername()) ?
                        (replier.equals(originalPoster) ? "yourself" : "you")
                        : originalPoster,
                isSent
        );
        // Add the wrapper to the message area
        if(inverted) {
            add(replyInfo,0);
        }else{
            add(replyInfo);
        }
    }

    // Returns a new reply panel based on the message dto
    public ReplyMessagePanel createReplyPanelFromMessageDTO(MessageDTO replyDTO) {
        // Check if the message is a file message
        Path path;
        if(replyDTO.getMessageType().equals(MessageType.FILE_MESSAGE) && replyDTO.getContent()!=null) {
            // Build path to file and check if the file exists locally, if not fetch it from remote
            path = ChatClientGui.LOCAL_FILES_PATH.resolve("images").resolve(replyDTO.getContent());
            if(replyDTO.getId() != null && !Files.exists(path)) {
                getImageFromRemote.getMessageAttachment(replyDTO.getId());
            }
        }

        ReplyMessagePanel reply = new ReplyMessagePanel(replyDTO.getId(),
                        replyDTO.getSender().getUsername(),
                        DateConvertor.toHourMinString(replyDTO.getSentTime()),
                        replyDTO.getContent() == null ? "Message deleted" : replyDTO.getContent(),
                        // If message is file message provide the path to the message's image
                        (replyDTO.getMessageType().equals(MessageType.FILE_MESSAGE) && replyDTO.getContent() != null) ?
                                ChatClientGui.LOCAL_FILES_PATH.resolve("images").resolve(replyDTO.getContent()) :
                                null
                );
                reply.getRemoveButton().setVisible(false);
        return reply;
    }

    // Updates the message status in message bubbles for which a new status was received
    public void updateBubbleStatuses(List<MessageStatusChangeDTO> updatedStatuses) {
        // If there are no updated statuses do stop method
        if(updatedStatuses==null){
            return;
        }
        for(MessageStatusChangeDTO status : updatedStatuses) {
            messageBubbles.get(status.getMessageId()).updateStatusIcon(status.getStatus());
        }
    }

    // Returns a map of message statuses for each message sent by user(so where they are displayed)
    public Map<UUID, StatusType> getForRefreshStatus() {
        Map<UUID, StatusType> messagesAndStatuses = new HashMap<>();
        for(UUID messageId : messageBubbles.keySet()){
            if(messageBubbles.get(messageId).isSent()){
                messagesAndStatuses.put(messageId, messageBubbles.get(messageId).getStatus());
            }
        }
        return messagesAndStatuses;
    }

    // Getters and setters
    public List<MessageBubble> getPendingMessages() {
        return pendingMessages;
    }

    public Map<UUID, MessageBubble> getMessageBubbles() {
        return messageBubbles;
    }

    public Map<String, MessageStatusChangeDTO> getPendingStatuses() {
        return pendingStatuses;
    }

    // Sets the parent for the message scroll pane
    public void setParentScrollPane(UiComponentListener parentObject) {
        this.parentObject = parentObject;
    }

    // Sets the active chat for the message area
    public void setActiveChat(SidebarChatCard activeChat) {
        this.activeChat = activeChat;
    }

    // Sets the getImageFromRemoteMethod, only possible once
    public void setGetImageFromRemoteMethod(GetMessageAttachmentExecutor getImageFromRemote) {
        this.getImageFromRemote = getImageFromRemote;
    }

    @Override
    public void applyTheme() {
        Theme theme = ThemeManager.getTheme();
        setBackground(theme.primaryColor());
    }
}
