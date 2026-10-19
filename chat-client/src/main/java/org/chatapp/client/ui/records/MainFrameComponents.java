package org.chatapp.client.ui.records;

import org.chatapp.client.ui.components.*;

import javax.swing.*;

public record MainFrameComponents(
        JPanel contentPane,
        JPanel left,
        JPanel right,
        JPanel body,

        SidebarPanel sidebar,
        JScrollPane sidebarContainer,

        SearchBox searchBox,
        NavbarPanel navbar,

        MessageAreaPanel messageArea,
        MessageScrollPane messageScrollPane,
        ChatInputField chatInputBox
) { }
