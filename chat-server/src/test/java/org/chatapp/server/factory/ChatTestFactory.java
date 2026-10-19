package org.chatapp.server.factory;

import org.chatapp.common.model.ChatType;
import org.chatapp.server.entities.Chat;

import java.time.Instant;

public class ChatTestFactory {

    public static Chat createRegularChat() {
        return Chat.builder()
                .chatType(ChatType.REGULAR)
                .build();
    }

    public static Chat createGroupChat() {
        return Chat.builder()
                .chatType(ChatType.REGULAR)
                .build();
    }

    public static Chat createChatWithTimeStamps(Instant created, Instant updated) {
        return Chat.builder()
                .chatType(ChatType.REGULAR)
                .createdDate(Instant.parse("2026-06-24T12:00:00Z"))
                .updateDate(Instant.parse("2026-06-24T12:00:00Z"))
                .build();
    }
}
