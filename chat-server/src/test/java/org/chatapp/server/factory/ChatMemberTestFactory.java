package org.chatapp.server.factory;

import org.chatapp.common.model.ChatterRole;
import org.chatapp.server.entities.Chat;
import org.chatapp.server.entities.ChatMember;
import org.chatapp.server.entities.User;

import java.time.Instant;

public class ChatMemberTestFactory {

    public static ChatMember createChatMember(Chat chat, User user) {
        return ChatMember.builder()
                .chat(chat)
                .user(user)
                .blocked(false)
                .mutedUntil(null)
                .pinned(false)
                .role(ChatterRole.MEMBER)
                .build();
    }

    public static ChatMember createChatMemberWithTimestamps(Chat chat, User user, Instant created, Instant updated) {
        return ChatMember.builder()
                .chat(chat)
                .user(user)
                .blocked(false)
                .mutedUntil(null)
                .pinned(false)
                .role(ChatterRole.MEMBER)
                .createdDate(created)
                .updateDate(updated)
                .build();
    }
}
