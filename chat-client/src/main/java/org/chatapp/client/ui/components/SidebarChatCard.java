package org.chatapp.client.ui.components;

import org.chatapp.client.interfaces.SidebarClickHandler;
import org.chatapp.client.service.DateConvertor;
import org.chatapp.client.ui.MainFrame;
import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.MessageDTO;
import org.chatapp.common.model.MessageType;
import org.chatapp.common.model.UserDTO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class SidebarChatCard extends JPanel implements MouseListener, ThemedComponent {
    private final JPanel upperRow;
    private final JPanel lowerRow;
    private final JLabel chatNameLbl;
    private final JLabel lastMessageTimeLbl;
    private final JLabel lastMessageLbl;
    private final JLabel unreadLbl;
    private final SidebarClickHandler clickHandler;

    private ChatDTO chatDTO;
    private Color backgroundColor;

    public SidebarChatCard(ChatDTO chatDTO, SidebarClickHandler clickHandler, UserDTO activeUser) {
        super();
        this.clickHandler = clickHandler;
        this.chatDTO = chatDTO;
        backgroundColor = ThemeManager.getTheme().sidebarColor();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Adds mouse listener that loads all the messages in the given chat to the main panel
        addMouseListener(this);

        // Create two JPanels that represent two rows of the chat card
        upperRow = new JPanel();
        upperRow.setLayout(new BoxLayout(upperRow, BoxLayout.X_AXIS));
        lowerRow = new JPanel();
        lowerRow.setLayout(new BoxLayout(lowerRow, BoxLayout.X_AXIS));

        // Create labels to show chat data
        chatNameLbl = new JLabel(chatDTO.getChatName(activeUser));

        if(chatDTO.getLastMessageContent() != null || chatDTO.getLastMessageSentTime() != null) {
            if(chatDTO.getLastMessageContent() == null) {
                lastMessageLbl = new JLabel("Message deleted");
            } else {
                lastMessageLbl = new JLabel(shortenText(chatDTO.getLastMessageContent()));
            }

            // Get only the time instead of date and time, in the future move this to message class and have methods for
            // both getting only the time and only the date
            DateTimeFormatter newFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
            String sentTimeFormatted = newFormat.format(chatDTO.getLastMessageSentTime());

            lastMessageTimeLbl = new JLabel(createSentTimeString());
        } else{
            lastMessageLbl = new JLabel("No messages");
            lastMessageTimeLbl = new JLabel("");
        }

        // If there are some unread messages show them in the custom unread messages label,
        // otherwise create an empty rounded label so it can be filled later if needed
        unreadLbl = new UnreadLabel(chatDTO.getUnreadMessages() != 0 ? Integer.toString(chatDTO.getUnreadMessages()) : "", 15);
        unreadLbl.setBackground(new Color(30, 183, 229));
        unreadLbl.setPreferredSize(new Dimension(30, 10));
        unreadLbl.setHorizontalAlignment(SwingConstants.CENTER);
        unreadLbl.setForeground(Color.WHITE);

        // Set custom fonts for labels not using default size font
        chatNameLbl.setFont(new Font("Open Sans", Font.BOLD, 16));
        lastMessageTimeLbl.setFont(new Font("Open Sans", Font.PLAIN, 14));
        unreadLbl.setFont(new Font("Open Sans", Font.BOLD, 14));

        // Add the labels to the rows with horizontal glue to have maximum space between them
        upperRow.add(chatNameLbl);
        upperRow.add(Box.createHorizontalGlue());
        upperRow.add(lastMessageTimeLbl);

        lowerRow.add(lastMessageLbl);
        lowerRow.add(Box.createHorizontalGlue());
        lowerRow.add(unreadLbl);

        // Add empty borders to the two rows for padding purposes
        upperRow.setBorder(BorderFactory.createEmptyBorder(10,10,5,10));
        lowerRow.setBorder(BorderFactory.createEmptyBorder(5,10,10,10));

        add(upperRow);
        add(Box.createVerticalGlue());
        add(lowerRow);

        // Set lower border to create horizontal line effect
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(70, 101, 124)));

        // Set background colors
        setBackground(backgroundColor);
        upperRow.setBackground(backgroundColor);
        lowerRow.setBackground(backgroundColor);
    }

    // Changes to color of the sidebar chat card (used for setting a different color for the opened chat)
    public void changeBackgroundColor(Color backgroundColor){
        this.backgroundColor = backgroundColor;
        setBackground(backgroundColor);
        upperRow.setBackground(backgroundColor);
        lowerRow.setBackground(backgroundColor);
    }

    // Updates the number of unread messages
    public void updateUnread(int unreadMessages) {
        chatDTO.setUnreadMessages(unreadMessages);
        if(unreadMessages == 0) {
            unreadLbl.setText("");
        } else {
            unreadLbl.setText(Integer.toString(chatDTO.getUnreadMessages()));
        }
        revalidate();
        repaint();
    }

    public void incrementUnread(){
        chatDTO.setUnreadMessages(chatDTO.getUnreadMessages() + 1);
        unreadLbl.setText(Integer.toString(chatDTO.getUnreadMessages()));
        revalidate();
        repaint();
    }

    // When a new message is sent in a chat this updates the last message preview and last message sent time in the
    // sidebar chat card
    public void updateLastMessage(MessageDTO lastMessage){
        if(lastMessage.getMessageType().equals(MessageType.FILE_MESSAGE)) {
            chatDTO.setLastMessageContent("Image message");
            chatDTO.setLastMessageSentTime(lastMessage.getSentTime());
            lastMessageLbl.setText("Image message");
        } else {
            chatDTO.setLastMessageContent(lastMessage.getContent());
            chatDTO.setLastMessageSentTime(lastMessage.getSentTime());
            lastMessageLbl.setText(shortenText(lastMessage.getContent()));
        }

        // Refresh last message time label
        lastMessageTimeLbl.setText(createSentTimeString());
        repaint();
        revalidate();
    }

    // Method used to handle the message deletion if the message is the last message sent in the chat
    public void handleLastMessageDeleted(){
        chatDTO.setLastMessageContent(null);
        lastMessageLbl.setText("Message deleted");

        repaint();
        revalidate();
    }

    // This class decides how to display the last message time/date. For example showing the hours if the message was
    // sent in the last 24 hours, otherwise showing yesterday, otherwise showing day of week, -||- date, -||- year
    private String createSentTimeString() {
        // Convert last message sent time to local time and get current time
        LocalDateTime sentTime = DateConvertor.toLocalDateTime(chatDTO.getLastMessageSentTime());
        LocalDateTime rightNow = LocalDateTime.now();

        // Find the time elapsed from last message and current time in minutes, hours and days
        long minutesAgo = sentTime.until(rightNow, ChronoUnit.MINUTES);
        long hoursAgo = sentTime.until(rightNow, ChronoUnit.HOURS);
        long daysAgo = sentTime.until(rightNow, ChronoUnit.DAYS);

        String text = "";

        // Set last message sent time label content based on time passed since last message in hours
        if(minutesAgo<1) {
            text = "Right now";
        } else if (minutesAgo < 60) {
            text = minutesAgo + (minutesAgo == 1 ? " minute " : " minutes ") + "ago";
        } else if (hoursAgo < 24) {
            text = sentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if(daysAgo == 1) {
            text = "Yesterday";
        } else if(daysAgo < 7) {
            text = sentTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        } else if(sentTime.getYear() == rightNow.getYear()) {
            text =  sentTime.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + sentTime.getDayOfMonth();
        } else {
            text =  sentTime.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " "
                    + sentTime.getDayOfMonth() + ", " + sentTime.getYear();
        }

        return text;
    }

    // Recalculates the refresh last message time label text and refreshes the label
    public void refreshLastMessageTimeLbl() {
        // When running the refresh from sidebar we call it for every chat cards where last message sent time is null
        // are skipped here
        if(chatDTO.getLastMessageSentTime() == null) {
            return;
        }
        lastMessageTimeLbl.setText(createSentTimeString());
        lastMessageTimeLbl.repaint();
        lastMessageTimeLbl.revalidate();
    }

    // Helper method to shorten the text shown in the last message label if the message is longer than expected
    private String shortenText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        if (text.length() > 21) {
            return text.substring(0, 21).concat("...");
        }

        return text;
    }

    public ChatDTO getChat() {
        return chatDTO;
    }

    public void setChat(ChatDTO chatDTO) {
        this.chatDTO = chatDTO;
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (backgroundColor.equals(ThemeManager.getTheme().sidebarActiveColor())){
            return;
        }
        clickHandler.handleSidebarClick(this);
        updateUnread(0);
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        // Not used
//        System.out.println("Mouse pressed");
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        // Not used
//        System.out.println("Mouse released");
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
        // Not used
//        System.out.println("Mouse entered");
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
        // Not used
//        System.out.println("Mouse exited");
    }

    @Override
    public void applyTheme() {
        backgroundColor = ThemeManager.getTheme().sidebarColor();
        changeBackgroundColor(backgroundColor);
    }
}
