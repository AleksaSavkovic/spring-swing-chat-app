package org.chatapp.client.ui.helpers;

import org.chatapp.client.ui.MainFrame;
import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.ui.components.*;
import org.chatapp.client.ui.records.MainFrameComponents;
import org.chatapp.client.ui.records.Theme;
import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.UserDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.List;
import java.awt.*;

public class MainFrameLayoutBuilder {

    private final MainFrame frame;
    private final UserDTO activeUser;
    private final List<ChatDTO> chatDTOList;
    private final Runnable sendMessageAction;
    private SidebarChatCard firstActiveChat;
    private final Theme theme;

    public MainFrameLayoutBuilder(MainFrame frame, UserDTO activeUser, List<ChatDTO> chatDTOList, Runnable sendMessageAction) {
        this.frame = frame;
        this.activeUser = activeUser;
        this.chatDTOList = chatDTOList;
        this.sendMessageAction = sendMessageAction;

        theme = ThemeManager.getTheme();
    }

    // Content pane setup and child object creation
    public MainFrameComponents build() {
        SearchBox searchBox = createSearchBox();
        NavbarPanel navbar = new NavbarPanel(theme.navbarColor(), activeUser, frame);

        SidebarPanel sidebar = createSidebar();
        JScrollPane sidebarContainer = createSidebarContainer(sidebar);

        // Creating a MessageAreaPanel to hold the message bubbles
        MessageAreaPanel messageArea = new MessageAreaPanel(activeUser);
        // Creating the scroll pane which will display all the messages
        MessageScrollPane messageScrollPane = new MessageScrollPane(messageArea, frame);
        // Creating the custom panel with rounder corners which holds the input field and button
        ChatInputField chatInputBox = new ChatInputField(sendMessageAction);

        JPanel body = createMainPanel(messageArea, messageScrollPane, chatInputBox);

        JPanel left = new JPanel(new BorderLayout());
        left.add(searchBox, BorderLayout.NORTH);
        left.add(sidebarContainer, BorderLayout.CENTER);
        left.setBackground(theme.navbarColor());

        JPanel right = new JPanel(new BorderLayout());
        right.add(navbar, BorderLayout.NORTH);
        right.add(body, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                left,
                right
        );

        split.setResizeWeight(0.28);
        split.setDividerSize(0);
        split.setBorder(null);
        split.setContinuousLayout(true);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(theme.primaryColor());
        contentPane.add(split, BorderLayout.CENTER);

        return new MainFrameComponents(
                contentPane,
                left,
                right,
                body,
                sidebar,
                sidebarContainer,
                searchBox,
                navbar,
                messageArea,
                messageScrollPane,
                chatInputBox
        );
    }

    // Create search bar JPanel and its components
    private SearchBox createSearchBox() {
        return new SearchBox(frame);
    }

    // Create the sidebar with the chat overview sidebar cards
    private SidebarPanel createSidebar() {
        SidebarPanel sidebar = new SidebarPanel(frame, activeUser);
        firstActiveChat = sidebar.addChatCards(chatDTOList);
        return sidebar;
    }

    // Create the scrollable sidebar container
    private JScrollPane createSidebarContainer(SidebarPanel sidebar) {
        JScrollPane scrollPane = new JScrollPane(sidebar);
        scrollPane.getViewport().setBackground(theme.sidebarColor());

        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.setBorder(new EmptyBorder(5, 0, 0, 0));

        return scrollPane;
    }

    // This method creates the main panel containing a chat input field as well as all the incoming and sent messages
    private JPanel createMainPanel(
            MessageAreaPanel messageArea,
            MessageScrollPane messageScrollPane,
            ChatInputField chatInputBox
    ) {
        JPanel body = new JPanel();
        body.setBackground(theme.primaryColor());
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(
                new EmptyBorder(20, 20, 20, 20)
        );


        body.add(messageScrollPane);
        body.add(Box.createVerticalStrut(10));
        body.add(chatInputBox);

        return body;
    }

    // Return the active chat card that was set during the build of mainframe ui components
    public SidebarChatCard getFirstActiveChat() {
        return firstActiveChat;
    }
}
