package org.chatapp.client.ui.components;

import org.chatapp.client.service.OnlineUserTracker;
import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.client.interfaces.UiComponentListener;
import org.chatapp.common.model.ChatMemberDTO;
import org.chatapp.common.model.ChatType;
import org.chatapp.common.model.UserDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Objects;

public class NavbarPanel extends JPanel implements ThemedComponent {

    private Color navbarColor;
    private final JLabel activeChatNameLbl;
    private final UiComponentListener parentObject;
    private JButton logoutButton;

    public NavbarPanel(Color navbarColor, UserDTO activeUser, UiComponentListener parentObject) {
        super();
        this.navbarColor = ThemeManager.getTheme().navbarColor();
        this.parentObject = parentObject;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setMinimumSize(new Dimension(0, 50));

        // Adding some padding to the left and right sides of the navbar container
        setBorder(new EmptyBorder(0,20,0,20));

        // Adding components to navbar
        // Create active chat name label with dummy text because it's value gets set during sidebar initialization
        activeChatNameLbl = new JLabel("<html>Current chat<br>Offline/Online</html>");
        add(activeChatNameLbl);
        add(Box.createHorizontalGlue());
        // Switch theme on theme toggle click
        ThemeToggleSwitch themeToggle = new ThemeToggleSwitch(80, 36, 8, 32, parentObject);

        add(themeToggle);
        // Add small gap between the toggle and active user username
        add(Box.createHorizontalStrut(10));
        add(new JLabel(activeUser.getUsername()));
        setLogoutButton();
        add(logoutButton);
        setBackground(navbarColor);
    }

    // Creates the logout button with the logout icon
    private void setLogoutButton() {
        ImageIcon logoutIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/icons/logout-icon.png"))
        );
        logoutButton = new RoundButton(logoutIcon);
        logoutButton.setMaximumSize(new Dimension(50, 50));
        logoutButton.setPreferredSize(new Dimension(50,50));
        logoutButton.setBackground(navbarColor);
        logoutButton.setBorder(new EmptyBorder(5,5,5,5));
        logoutButton.addActionListener(e -> {
            handleLogout();
        });
    }

    public void setActiveChatNameLabel(SidebarChatCard activeChat, OnlineUserTracker onlineUserTracker, UserDTO activeUser){
        // If this starts before any chats are loaded
        if(activeChat == null) {
            return;
        }
        StringBuilder labelText = new StringBuilder();
        String chatName = activeChat.getChat().getChatName(activeUser);

        // Check if the active chat is a regular or group chat. For regular chat we just show Online/Offline in green/red
        // for group chat show few users which are online and the number of total users online similar to this:
        // "john_johnson, lars_larson and N others online".
        if(activeChat.getChat().getChatType().equals(ChatType.REGULAR)) {
            setRegularChatLabel(activeChat, onlineUserTracker, activeUser, labelText, chatName);
        } else {
            setGroupChatLabel(activeChat, onlineUserTracker, labelText);
        }
        getChatNameLbl().setText(labelText.toString());
    }

    private void setRegularChatLabel(SidebarChatCard activeChat, OnlineUserTracker onlineUserTracker, UserDTO activeUser, StringBuilder labelText, String chatName) {
        // Get the chatter from the active chat which is not the current user
        ChatMemberDTO otherUser = activeChat.getChat().getUsers()
                .stream()
                .filter(chatMemberDTO -> !chatMemberDTO.getUserId().equals(activeUser.getId()))
                .findFirst().orElseThrow(() -> new RuntimeException("Unexpected error, second user of the chat not found!"));
        // Check if the chatter is online
        boolean online = onlineUserTracker.getUserStatus(otherUser.getUserId());

        labelText.append("<html>").append(chatName).append("<br><font color=").append(online ? "'green'>" : "'red'>")
                .append(online ? "Online" : "Offline").append("</font></html>");
    }

    private void setGroupChatLabel(SidebarChatCard activeChat, OnlineUserTracker onlineUserTracker, StringBuilder labelText) {
        // Get the online users for chat and save the number of them
        List<ChatMemberDTO> onlineUsers = onlineUserTracker.getOnlineUsersForChat(activeChat.getChat());
        int count = onlineUsers.size();

        // In case there are 0 users online just show static red text saying all users offline
        if(count == 0) {
            labelText.append("<html>")
                    .append(activeChat.getChat().getChatName()).append("<br><font color=").append("'red'>")
                    .append("All users offline").append("</font></html>");
        } else {
            // Add chat name and font color green because there is at least 1 user online
            labelText.append("<html>").append(activeChat.getChat().getChatName())
                    .append("<br><font color=").append("'green'>");
            // Go through the list add the username of the user and decrease the count by one do this 3 times max
            for(int i = 0;i < onlineUsers.size(); i++) {
                // When we reach three usernames displayed we stop displaying them
                if (i == 3){
                    break;
                }
                labelText.append(onlineUsers.get(i).getUsername());
                labelText.append(", ");
                count--;
            }
            // If there are some other users online after we displayed our 3 we show " and X others online"
            if(count > 0) {
                labelText.append(" and ").append(count)
                        // If it's 1 we say and "1 other online" otherwise it's "X others online"
                        .append(count != 1 ? " others online</font></html>" : " other online</font></html>");
            } else {
                // In the case there are 0 other users online we remove ", " from the end of the string builder that
                // were added after the last append in the for loop
                labelText.delete(labelText.length() - 2, labelText.length());
                labelText.append(" online</font></html>");
            }
        }
    }

    // Fires of logout event to MainFrame
    public void handleLogout() {
        SwingUtilities.invokeLater(parentObject::onLogoutEvent);
    }

    // Returns the label showing active chat name in navbar
    public JLabel getChatNameLbl(){
        return activeChatNameLbl;
    }

    @Override
    public void applyTheme() {
        navbarColor = ThemeManager.getTheme().navbarColor();
        setBackground(navbarColor);
        logoutButton.setBackground(navbarColor);
    }
}
