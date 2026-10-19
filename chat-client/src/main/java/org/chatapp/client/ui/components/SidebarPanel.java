package org.chatapp.client.ui.components;

import org.chatapp.client.interfaces.SidebarClickHandler;
import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.client.ui.records.Theme;
import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.ChatType;
import org.chatapp.common.model.UserDTO;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;

public class SidebarPanel extends JPanel implements Scrollable, ThemedComponent {

    private Color sidebarColor;
    private Color sidebarActiveColor;
    private final SidebarClickHandler clickHandler;
    private final Map<UUID, SidebarChatCard> chatCards;
    private SidebarChatCard activeChat;
    private final UserDTO activeUser;

    // Timer used for refreshing last message sent time labels in sidebar chat cards
    private final Timer timestampTimer;


    public SidebarPanel(SidebarClickHandler clickHandler, UserDTO activeUser) {
        super();
        Theme theme = ThemeManager.getTheme();
        sidebarColor = theme.sidebarColor();
        sidebarActiveColor = theme.sidebarActiveColor();
        this.clickHandler = clickHandler;
        this.activeUser = activeUser;
        setBackground(sidebarColor);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        // Initializing map for storing chat cards with the chat id being used as key
        chatCards = new HashMap<UUID, SidebarChatCard>();

        // Timer that refreshes chat card last sent time labels every minute
        timestampTimer = new Timer(
                60_000,
                e -> refreshSentTimes()
        );

        timestampTimer.start();

    }

    // Adds all the chat cards to the chat panel for the first time and returns the top panel
    public SidebarChatCard addChatCards(List<ChatDTO> chatDTOList){
        SidebarChatCard firstPanel = null;

        // Sort the chats so they are shown in descending order of last message time
        Collections.sort(chatDTOList);

        for (int i = 0; i < chatDTOList.size(); i++) {
            // Skip the chat if it's a regular chat, and is not shown in the database
            if(chatDTOList.get(i).getChatType().equals(ChatType.REGULAR) && chatDTOList.get(i).getLastMessageSentTime() == null) {
                continue;
            }
            // Create chat card for each chat in the chat dto list
            SidebarChatCard chatPanel = addChatToSidebar(chatDTOList.get(i));
            // Add each chat sidebar card to the chatCards map
            chatCards.put(chatPanel.getChat().getId(), chatPanel);

            // Automatically open the first chat in the sidebar list
            if(i == 0) {
                firstPanel = chatPanel;
            }
        }
        return firstPanel;
    }

    // Refresh the sidebar by removing all chat sidebar cards and adding the new updated ones
    public SidebarChatCard refreshSidebar(List<ChatDTO> chatDTOList) {
        removeAll();
        chatCards.clear();

        // Sort the chats so they are shown in descending order of last message time
        Collections.sort(chatDTOList);
        SidebarChatCard firstPanel = null;
        for (int i = 0; i < chatDTOList.size(); i++) {
            // Skip the chat if it's a regular chat, and is not shown in the database
            if(chatDTOList.get(i).getChatType().equals(ChatType.REGULAR) && chatDTOList.get(i).getLastMessageSentTime() == null) {
                continue;
            }
            SidebarChatCard chatPanel = addChatToSidebar(chatDTOList.get(i));
            chatCards.put(chatPanel.getChat().getId(), chatPanel);

            // Move the showChatMessages to call to mainframe refreshSidebar method
            if (i == 0) {
                firstPanel = chatPanel;
            }
        }

        revalidate();
        repaint();

        return firstPanel;
    }

    // Set the active chat in the chat sidebar
    public void setActiveChat(SidebarChatCard activeChat) {
        if(this.activeChat != null) {
            this.activeChat.changeBackgroundColor(sidebarColor);
        }

        this.activeChat = activeChat;
        this.activeChat.changeBackgroundColor(sidebarActiveColor);
        revalidate();
        repaint();
    }

    // Create chat card and add it to the sidebar
    public SidebarChatCard addChatToSidebar(ChatDTO chat) {
        SidebarChatCard chatPanel = new SidebarChatCard(chat, clickHandler, activeUser);
        chatCards.put(chat.getId(), chatPanel);
        add(chatPanel);
        revalidate();
        repaint();

        return chatPanel;
    }

    // Refreshes last message sent label text for each sidebar chat card
    public void refreshSentTimes(){
        for(SidebarChatCard chatCard : chatCards.values()) {
            chatCard.refreshLastMessageTimeLbl();
        }
    }

    // Returns the chat card with the given chat id
    public SidebarChatCard getChatCard(UUID chatId) {
        return chatCards.get(chatId);
    }

    // Returns the first sidebar chat card
    public SidebarChatCard getFirstChat() {
        return (SidebarChatCard) getComponent(0);
    }


    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return null;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle rectangle, int i, int i1) {
        return 10;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle rectangle, int i, int i1) {
        return 100;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public void applyTheme() {
        Theme theme = ThemeManager.getTheme();
        sidebarColor = theme.sidebarColor();
        sidebarActiveColor = theme.sidebarActiveColor();
        setBackground(sidebarColor);
    }
}
