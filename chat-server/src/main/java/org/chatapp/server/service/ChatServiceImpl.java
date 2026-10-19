package org.chatapp.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.common.model.*;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.entities.Chat;
import org.chatapp.server.entities.Message;
import org.chatapp.server.entities.User;
import org.chatapp.server.mappers.ChatMapper;
import org.chatapp.server.mappers.ChatMemberMapper;
import org.chatapp.server.repositories.ChatMemberRepository;
import org.chatapp.server.repositories.ChatRepository;
import org.chatapp.server.repositories.MessageRepository;
import org.chatapp.server.repositories.MessageStatusRepository;
import org.chatapp.server.validation.ChatValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("chatService")
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatValidator chatValidator;

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;
    private final ChatMemberRepository chatMemberRepository;

    private final ChatMapper chatMapper;
    private final ChatMemberMapper chatMemberMapper;

    // Method used for finding the recipients for a message (all chat members in the chat excluding the sender)
    @Override
    public List<User> getRecipients(UUID chatId, UUID senderId) {
        return chatMemberRepository.findUsersByChatId(chatId).stream().filter(user -> !user.getId().equals(senderId)).toList();
    }

    // Method that returns all chats that the user is member of
    @Transactional(readOnly = true)
    @Override
    public List<ChatDTO> listUserChats(UUID userId) {
        return chatRepository.findAllByUsersUserId(userId).stream().map(chat -> toChatDto(chat, userId)).toList();
    }

    // Returns a map of all chats for the IDs in the list along with the chat members of each chat
    @Override
    public Map<UUID,Chat> getChatsWithUsers(List<UUID> chatIds) {
        // Create map based on set of values obtained from repository
        return chatRepository.findChatsByIdsWithUsers(chatIds).stream().collect(Collectors.toMap(Chat::getId, chat -> chat));
    }

    // Method used to handle adding last message and unread message count to the chat DTO
    @Override
    public ChatDTO toChatDto(Chat entity, UUID userId) {

        ChatDTO dto = chatMapper.toDto(entity);

        // Add last message and unread messages to the chat dto
        Optional<Message> lastMessage = messageRepository.findTopByChatOrderByCreatedDateDesc(entity);
        if(lastMessage.isPresent()){
            dto.setLastMessageContent(lastMessage.get().getMessageType().equals(MessageType.FILE_MESSAGE) ? "Image message" : lastMessage.get().getContent());
            dto.setLastMessageSentTime(lastMessage.get().getCreatedDate());
        }
        dto.setUnreadMessages(messageStatusRepository.countUnreadMessages(entity.getId(), userId));

        return dto;
    }

    @Override
    public Chat toChatEntity(ChatDTO dto) {
        Chat entity = chatMapper.toEntity(dto);

        // Adding all the users that were added to the group chat during creation to the entity
        if(dto.getUsers() != null && !dto.getUsers().isEmpty()) {
            entity.addUsers(dto.getUsers().stream().map(chatMemberMapper::toEntity).collect(Collectors.toSet()));
        }

        return entity;
    }

    @Override
    public List<Chat> getNewChats(UUID userId, Instant lastSync){
        return chatRepository.syncUserChatsSince(userId, lastSync);
    }

    @Transactional
    @Override
    public ChatDTO saveChat(ChatDTO chat) {
        // Validates chat and throws exception if required data is not there
        chatValidator.validateChatCreate(chat);

        // Check chat type, if chat type is REGULAR check for a chat with same 2 users in the repository
        if(chat.getChatType().equals(ChatType.REGULAR)){
            Optional<Chat> foundChat = chatRepository.findChatWithUsers(
                    chat.getUsers().get(0).getUserId(), chat.getUsers().get(1).getUserId());
            // Return existing chat if found instead of creating it again
            if(foundChat.isPresent()){
                return chatMapper.toDto(foundChat.get());
            }
        }
        return chatMapper.toDto(chatRepository.save(toChatEntity(chat)));
    }

    // Checks if the user with the userId is a member of chat with chatId
    @Transactional
    @Override
    public boolean isChatMember(UUID chatId, UUID userId) {
        Chat foundChat = chatRepository.findById(chatId).orElseThrow(NotFoundException::new);

        return foundChat.getUsers().stream().anyMatch(chatMember -> chatMember.getUser().getId().equals(userId));
    }

    @Transactional
    @Override
    public Optional<ChatDTO> findChatById(UUID chatId) {
        return Optional.of(chatMapper.toDto(chatRepository.findById(chatId).orElseThrow(NotFoundException::new)));
    }

    @Override
    public Optional<Chat> findChatEntityById(UUID chatId) {
        return chatRepository.findById(chatId);
    }

}
