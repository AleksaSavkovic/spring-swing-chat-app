package org.chatapp.client.interfaces;

import org.chatapp.client.ui.components.MessageBubble;

public interface UiComponentListener {

    void onScrollToBottom();

    void onScrollToTop(int pageNumber, int pageSize);

    void onLogoutEvent();

    void onMessageMenuEvent(String event, MessageBubble messageBubble);

    void onThemeChange(String themeName);
}
