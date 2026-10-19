package org.chatapp.server.service;


import org.chatapp.common.model.ChatDTO;
import org.chatapp.server.entities.Chat;
import org.chatapp.server.entities.User;

import java.time.Instant;
import java.util.*;

public interface ChatService {

    Optional<ChatDTO> findChatById(UUID chatId);

    Optional<Chat> findChatEntityById(UUID chatId);

    List<User> getRecipients(UUID chatId, UUID senderId);

    List<ChatDTO> listUserChats(UUID userId);

    Map<UUID, Chat> getChatsWithUsers(List<UUID> chatIds);

    ChatDTO toChatDto(Chat chat, UUID userId);

    Chat toChatEntity(ChatDTO dto);

    ChatDTO saveChat(ChatDTO chat);

    boolean isChatMember(UUID chatId, UUID userId);

    List<Chat> getNewChats(UUID userId, Instant lastSync);

}
