package org.chatapp.client.ui.records;

import java.awt.*;

public record Theme(
        Color primaryColor,
        Color navbarColor,
        Color sidebarColor,
        Color sidebarActiveColor,
        Color searchbarColor,
        Color searchPromptColor,
        Color bubbleColor,
        Color activeBubbleColor,
        Color modalBackground,
        Color textColor,
        Color deletedBubbleColor
) {}
