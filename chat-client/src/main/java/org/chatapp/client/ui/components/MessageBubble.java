package org.chatapp.client.ui.components;

import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.client.interfaces.UiComponentListener;
import org.chatapp.client.ui.records.Theme;
import org.chatapp.common.model.StatusType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public class MessageBubble extends JPanel implements ThemedComponent {

    // Constants
    private Color receivedColor;
    private Color sentColor;
    private static final int MAX_WIDTH = 400;
    private static final int MIN_WIDTH = 80;
    public static final Dimension MAX_IMAGE_DIMENSION = new Dimension(450, 450);
    public static final Dimension MIN_IMAGE_DIMENSION = new Dimension(100, 100);


    // Parent object for passing on specific events
    private final UiComponentListener parentObject;

    // Panel containing the message label, image label and reply panel
    private final JPanel contentWrapper;
    // Message text area and label displaying time and status
    private final JTextArea msgLabel;
    private JLabel imageLabel;
    private final JLabel timeAndStatusLabel;
    private final JLabel userLabel;
    private ReplyMessagePanel replyPanel;

    // This is used for bulk updating the status for this user
    private final String username;
    // This is used for sorting message bubbles where message area is being refreshed
    private final Instant sentTime;
    // This is used for checking whether to position the bubble on the left or right
    private final boolean isSent;
    // This is used for checking what is the current status for the message
    private StatusType status;
    // This is used to check if the message "show recipients statuses" menu item should be shown (only for group chats)
    private final boolean groupChatMsg;
    private UUID messageId;
    private String messageContent;
    // This is used to check if a message bubble is actually an image message bubble, and if it is to get the path to the image
    private final Path messageAttachment;

    // Popup menu that opens on right click of message
    private final JPopupMenu messagePopupMenu;

    public MessageBubble(String msgContent, Instant sentTime, boolean isSent, boolean groupChatMsg,
                         String username, StatusType status, UiComponentListener parentObject, Path messageAttachment, ReplyMessagePanel reply){
        super();
        setLayout(new BorderLayout());
        // Set class fields
        this.username = username;
        this.sentTime = sentTime;
        this.isSent = isSent;
        this.status = status;
        this.parentObject = parentObject;
        this.groupChatMsg = groupChatMsg;
        this.messageContent = msgContent;
        this.replyPanel = reply;
        this.messageAttachment = messageAttachment;

        // Getting color theme from theme manager and setting the color variables' values
        Theme theme = ThemeManager.getTheme();
        receivedColor = theme.bubbleColor();
        sentColor = theme.activeBubbleColor();

        // Fill message bubble with appropriate content
        if(msgContent == null) {
            msgLabel = new JTextArea("Message deleted");
        } else if(messageAttachment != null) {
            msgLabel = new JTextArea("Loading image...");
        } else {
            msgLabel = new JTextArea(messageContent);
        }

        contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBorder(null);
        contentWrapper.setOpaque(false);
        if(reply != null) {
            contentWrapper.add(replyPanel, BorderLayout.NORTH);
        }
        contentWrapper.add(msgLabel, BorderLayout.CENTER);

        messageContentLabel(theme);

        // Creating the message popupmenu and its items
        messagePopupMenu = getJPopupMenu();
        // Add mouse listener for opening the popup menu
        addPopupMenuListener();

        // Make this an attribute of the method
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
        String formatedTime = timeFormat.format(sentTime);

        // Check if the message is in a group chat if so add the username to top left corner of the message bubble
        if (groupChatMsg && !isSent) {
            userLabel = new JLabel("  " + username, SwingConstants.LEFT);
            userLabel.setForeground(sentColor);
            if(replyPanel!=null) {
                userLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
            }
            add(userLabel, BorderLayout.NORTH);
        } else {
            userLabel = null;
        }

        // Set color based on if the message was sent or received, as well as setting the status icon if it was sent
        if (isSent) {
            setBackground(sentColor);
            ImageIcon statusIcon = new ImageIcon(
                    Objects.requireNonNull(getClass().getResource(getIconPathForStatus(status)))
            );
            timeAndStatusLabel = new JLabel(formatedTime, statusIcon, SwingConstants.RIGHT);
        } else {
            setBackground(receivedColor);
            timeAndStatusLabel = new JLabel(formatedTime, SwingConstants.RIGHT);
        }
        if(messageContent == null) {
            setBackground(theme.deletedBubbleColor());
        }
        timeAndStatusLabel.setForeground(theme.textColor());

        // Add message label containing the message content to the message bubble
        add(contentWrapper, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        setOpaque(false);

        // Add timeAndStatus label containing sent time and possibly message status to the message bubble
        timeAndStatusLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        add(timeAndStatusLabel, BorderLayout.SOUTH);

        // Control alignment within BoxLayout
        setAlignmentX(isSent ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

        // Setup bubble dimensions
        calculateBubbleDimensions(userLabel);
    }

    // Calculate the appropriate bubble dimensions based on the message content
    private void calculateBubbleDimensions(JLabel userLabel) {
        int verticalPadding;
        // Calculating bubble sized based on content size
        int textWidth =  msgLabel.getFontMetrics(msgLabel.getFont()).stringWidth(msgLabel.getText());
        int lines = (int) Math.ceil((double) textWidth / (MAX_WIDTH - 24));
        int lineHeight = msgLabel.getFontMetrics(msgLabel.getFont()).getHeight();
        // Add more space in the message bubble if a username is going to be displayed in it
        if(userLabel != null) {
            lineHeight+= userLabel.getFontMetrics(userLabel.getFont()).getHeight();
        }

        // Setting height and width with added padding
        int height = lineHeight * Math.max(1, lines) + 20;
        int width = Math.min(MAX_WIDTH, textWidth + 40);
        width = Math.max(MIN_WIDTH, width);
        if(replyPanel != null) {
            height+= (int)replyPanel.getPreferredSize().getHeight();
            width = Math.max(width, (int)replyPanel.getPreferredSize().getWidth() + 20);
            if(userLabel!=null) {
                height+=5;
            }
        }
        verticalPadding = 10;

        // Get the max value from the generated width or width of the username label, so
        // that if the message is too short the username is still visible
        if(userLabel != null) {
            width = Math.max(width, userLabel.getFontMetrics(userLabel.getFont()).stringWidth(userLabel.getText()));
        }

        // Added values to height and width to account for the padding
        setAbsoluteDimensions(width, height+verticalPadding);
    }

    // Styling the message content text area
    private void messageContentLabel(Theme theme) {
        msgLabel.setLineWrap(true);
        msgLabel.setWrapStyleWord(true);
        msgLabel.setEditable(false);
        msgLabel.setOpaque(false);
        // If message is deleted make the text gray and italic instead of theme color and bold
        if(messageContent == null) {
            msgLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        } else {
            msgLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        }
        msgLabel.setForeground(theme.textColor());
    }

    // Method used for setting preferred, maximum and minimum bubble size all ot once
    private void setAbsoluteDimensions(int width, int height) {
        Dimension newDimensions = new Dimension(width, height);
        setPreferredSize(newDimensions);
        setMaximumSize(newDimensions);
        setMinimumSize(newDimensions);
    }

    private JPopupMenu getJPopupMenu() {
        if(messagePopupMenu == null) {
            // Create JPopupMenu
            JPopupMenu messagePopupMenu = new JPopupMenu();

            // Create JMenuItems to be added to the menu
            // Check if message is in group chat before adding show recipient statuses menu item
            if(groupChatMsg) {
                JMenuItem recipientsItem = new JMenuItem("Show recipient statuses");
                recipientsItem.addActionListener(actionEvent ->
                        parentObject.onMessageMenuEvent("recipients", this));
                messagePopupMenu.add(recipientsItem);
            }
            JMenuItem replyItem = new JMenuItem("Reply to message");
            replyItem.addActionListener(actionEvent ->
                    parentObject.onMessageMenuEvent("reply", this));
            // Check if message is sent by active user before adding delete menu item
            if(isSent) {
                JMenuItem deleteItem = new JMenuItem("Delete message");
                deleteItem.addActionListener(actionEvent ->
                        parentObject.onMessageMenuEvent("delete", this));
                messagePopupMenu.add(deleteItem);
            }
            JMenuItem copyItem = new JMenuItem("Copy message");
            copyItem.addActionListener(actionEvent ->
                    parentObject.onMessageMenuEvent("copy", this));

            // Check if message contains an image before adding view image menu item
            if(messageAttachment != null) {
                JMenuItem viewImageItem = new JMenuItem("Open image");
                viewImageItem.addActionListener(actionEvent ->
                        parentObject.onMessageMenuEvent("view-image", this));
                messagePopupMenu.add(viewImageItem);
            }

            // Add the JMenuItems to the menu
            messagePopupMenu.add(replyItem);
            messagePopupMenu.add(copyItem);


            return messagePopupMenu;
        }
        return messagePopupMenu;
    }

    // Changes status icon based on the new message status
    public void updateStatusIcon(StatusType status){
        this.status = status;
        String iconPath = getIconPathForStatus(status);
        if(iconPath.isEmpty()){
            System.out.println("Error occurred finding the message status icon");
        }
        ImageIcon newIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource(iconPath))
        );
        timeAndStatusLabel.setIcon(newIcon);
        revalidate();
        repaint();
    }

    // Return the appropriate icon path for message status
    private String getIconPathForStatus(StatusType status) {
        String iconPath = "";

        if(status == null){
            iconPath = "/icons/sending-icon.png";
        }else if(status.equals(StatusType.SEEN)){
            iconPath = "/icons/seen-icon.png";
        } else if(status.equals(StatusType.DELIVERED)) {
            iconPath = "/icons/delivered-icon.png";
        } else if(status.equals(StatusType.SENT)){
            iconPath = "/icons/sent-icon.png";
        }

        return iconPath;
    }

    // Set the content of the bubble to null
    public void eraseBubbleContent() {
        messageContent = null;
        if(replyPanel != null) {
            contentWrapper.remove(replyPanel);
            replyPanel = null;
        }
        if(imageLabel != null) {
            imageLabel.setIcon(null);
            remove(imageLabel);
            add(msgLabel, BorderLayout.CENTER);
        }
        // Change label contents and style
        Theme theme = ThemeManager.getTheme();
        msgLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        setBackground(theme.deletedBubbleColor());
        msgLabel.setText("Message deleted");

        // Resize and refresh label
        calculateBubbleDimensions(userLabel);
        revalidate();
        repaint();
    }

    // Adds mouse listener that checks if mouse was clicked and if it was it opens the popupmenu
    public void addPopupMenuListener(){
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            // Checks if right click was pressed
            private void checkPopup(MouseEvent e) {
                if(e.isPopupTrigger()) {
                    messagePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    // Returns the contents of the message in the message bubble
    public String getBubbleContent(){
        return messageContent;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        g2.dispose();
    }

    // Returns the username of the sender of the message
    public String getSender() {
        return username;
    }

    // Returns the sentTime of the message
    public Instant getSentTime() {
        return sentTime;
    }

    // Returns true if the message was sent by active user
    public boolean isSent() {
        return isSent;
    }

    // Returns the status type currently displayed in message bubble
    public StatusType getStatus(){
        return status;
    }

    @Override
    public void applyTheme() {
        Theme theme = ThemeManager.getTheme();
        receivedColor = theme.bubbleColor();
        sentColor = theme.activeBubbleColor();
        msgLabel.setForeground(theme.textColor());
        timeAndStatusLabel.setForeground(theme.textColor());
        // Updates the ui for JPopupMenu so new flatlaf theme is applied
        messagePopupMenu.updateUI();

        // Set the bubble color based on message type
        if(messageContent == null) {
            setBackground(theme.deletedBubbleColor());
        } else if(isSent){
            setBackground(sentColor);
        } else {
            setBackground(receivedColor);
        }
    }

    // Get the message id
    public UUID getMessageId() {
        return messageId;
    }

    // Get the userLabel
    public JLabel getUserLabel() {
        return userLabel;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    // Returns the path to the message attachment on the local file system
    public Path getMessageAttachmentPath() {
        return messageAttachment;
    }

    // Returns the reply panel in this message, could be null
    public ReplyMessagePanel getReplyPanel() {
        return replyPanel;
    }

    // Add the new reply panel instead of the old one
    public void updateReplyPanel(ReplyMessagePanel newReplyPanel) {
        contentWrapper.remove(replyPanel);
        this.replyPanel = newReplyPanel;
        replyPanel.getRemoveButton().setVisible(false);
        contentWrapper.add(replyPanel, BorderLayout.NORTH);

        // Resize bubble after reply panel update
        calculateBubbleDimensions(userLabel);
        revalidate();
        repaint();
    }

    // Set the reply content to "Message deleted" and refresh the whole message bubble
    public void updateReplyContentOnDelete() {
        replyPanel.updateContentOnDelete();

        // Resize bubble after reply panel update
        if(imageLabel == null) {
            calculateBubbleDimensions(userLabel);
        } else {
            recalculatePanelDimensionsWithIcon((ImageIcon) imageLabel.getIcon());
        }
        revalidate();
        repaint();

    }

    // Set the image label image to the provided image icon
    public void setImageLabelImage(ImageIcon scaledIcon) {
        // If icon is null set the bubble icon to the message not found icon
        if(scaledIcon == null) {
            scaledIcon = new ImageIcon(
                        Objects.requireNonNull(getClass().getResource("/icons/image-not-found-icon.png"))
            );
        }

        // Resize panel
        recalculatePanelDimensionsWithIcon(scaledIcon);

        // Initialize image label with new icon, add it to message bubble and remove the msg label
        imageLabel = new JLabel(scaledIcon);
        contentWrapper.remove(msgLabel);
        contentWrapper.add(imageLabel, BorderLayout.CENTER);

        getParent().setMaximumSize(new Dimension((int)getParent().getMaximumSize().getWidth(), (int)getPreferredSize().getHeight() + 20));
        getParent().setPreferredSize(new Dimension((int)getParent().getPreferredSize().getWidth(), (int)getPreferredSize().getHeight() + 20));
        revalidate();
        repaint();
        getParent().revalidate();
        getParent().repaint();
    }

    // Calculates the dimensions of the message bubble taking the message image into consideration
    private void recalculatePanelDimensionsWithIcon(ImageIcon scaledIcon) {
        // Getting the image dimensions
        int height = scaledIcon.getIconHeight();
        int width = scaledIcon.getIconWidth();
        int verticalPadding = 40;

        if(replyPanel != null) {
            height+= (int)replyPanel.getPreferredSize().getHeight();
        }

        // Add values to height and width to account for the padding
        setAbsoluteDimensions(width, height+verticalPadding);
    }


}