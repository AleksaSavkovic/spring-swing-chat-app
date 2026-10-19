package org.chatapp.server.integration.service;

import org.chatapp.common.model.*;
import org.chatapp.common.model.ChatType;
import org.chatapp.common.model.ChatterRole;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.exceptions.ValidationException;
import org.chatapp.server.entities.*;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import({MyTestConfiguration.class, TestDataHelper.class})
public class ChatServiceIT {
    @Autowired
    ChatService chatService;
    @Autowired
    TestDataHelper testDataHelper;

    @Test
    void getRecipients_ShouldReturnFilterOutSender() {
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        // Given
        UUID chatId = groupChat1.getId();
        UUID senderId = user1.getId();

        // When
        List<User> recipients = chatService.getRecipients(chatId, senderId);

        // Then
        assertThat(recipients)
                .isNotEmpty()
                .extracting(User::getId)
                .containsExactlyInAnyOrder(
                        user2.getId(), user3.getId(), user4.getId()
                );
    }

    @Test
    void getRecipients_ShouldReturnEmptyList() {
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        // Given
        UUID chatId = UUID.randomUUID();
        UUID senderId = user1.getId();

        // When
        List<User> recipients = chatService.getRecipients(chatId, senderId);

        // Then
        assertThat(recipients)
                .isEmpty();
    }

    @Test
    void getChatsWithUsers_ShouldReturn3ChatsWithUsers(){
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        Chat groupChat2 = testDataHelper.saveGroupChat();
        ChatMember groupChat2Member1 = testDataHelper.saveChatMember(groupChat2, user1);
        ChatMember groupChat2Member2 = testDataHelper.saveChatMember(groupChat2, user2);
        ChatMember groupChat2Member3 = testDataHelper.saveChatMember(groupChat2, user3);

        // This chat should NOT be returned
        Chat regularChat2 = testDataHelper.saveRegularChat();
        testDataHelper.saveChatMember(regularChat2, user1);
        testDataHelper.saveChatMember(regularChat2, user3);

        // Given
        List<UUID> chatIds = List.of(groupChat1.getId(), regularChat1.getId(), groupChat2.getId());

        // When
        Map<UUID, Chat> chats = chatService.getChatsWithUsers(chatIds);

        // Then
        assertThat(chats.keySet())
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        groupChat1.getId(), regularChat1.getId(), groupChat2.getId()
                );

        // Check that all chats have their users (chat members)
        assertThat(chats.values())
                .hasSize(3)
                .flatMap(chat -> chat.getUsers().stream().map(ChatMember::getId).toList())
                .containsExactlyInAnyOrder(
                        groupChat1Member1.getId(), groupChat1Member2.getId(), groupChat1Member3.getId(),
                        groupChat1Member4.getId(), regularChat1Member1.getId(), regularChat1Member2.getId(),
                        groupChat2Member1.getId(), groupChat2Member2.getId(), groupChat2Member3.getId()
                );
    }

    @Test
    void getChatsWithUsers_ShouldReturnEmptyList(){
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        Chat groupChat2 = testDataHelper.saveGroupChat();
        ChatMember groupChat2Member1 = testDataHelper.saveChatMember(groupChat2, user1);
        ChatMember groupChat2Member2 = testDataHelper.saveChatMember(groupChat2, user2);
        ChatMember groupChat2Member3 = testDataHelper.saveChatMember(groupChat2, user3);

        Chat regularChat2 = testDataHelper.saveRegularChat();
        testDataHelper.saveChatMember(regularChat2, user1);
        testDataHelper.saveChatMember(regularChat2, user3);

        // Given
        List<UUID> chatIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        // When
        Map<UUID, Chat> chats = chatService.getChatsWithUsers(chatIds);

        // Then
        assertThat(chats.keySet())
                .isEmpty();
    }

    @Test
    void listUserChats_ShouldReturn2chats() {
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        Chat groupChat2 = testDataHelper.saveGroupChat();
        ChatMember groupChat2Member1 = testDataHelper.saveChatMember(groupChat2, user1);
        ChatMember groupChat2Member2 = testDataHelper.saveChatMember(groupChat2, user2);
        ChatMember groupChat2Member3 = testDataHelper.saveChatMember(groupChat2, user3);

        // This chat should NOT be returned
        Chat regularChat2 = testDataHelper.saveRegularChat();
        testDataHelper.saveChatMember(regularChat2, user1);
        testDataHelper.saveChatMember(regularChat2, user3);

        // Given
        UUID userId = user2.getId();

        // When
        List<ChatDTO> userChats = chatService.listUserChats(userId);

        // Then
        assertThat(userChats)
                .hasSize(3)
                .extracting(ChatDTO::getId)
                .containsExactlyInAnyOrder(
                        groupChat1.getId(), groupChat2.getId(), regularChat1.getId()
                );
    }

    @Test
    void toChatDto_ShouldReturnDTO(){
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);
        chat1.setUsers(Set.of(chat1Member1, chat1Member2));

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user1, chat1);
        Message message3 = testDataHelper.saveMessage(user2, chat1);
        Message message4 = testDataHelper.saveMessage(user2, chat1, "Special content");

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.DELIVERED);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user2, StatusType.SEEN);
        MessageStatus messageStatus3 = testDataHelper.saveMessageStatus(message3, user1, StatusType.SEEN);
        MessageStatus messageStatus4 = testDataHelper.saveMessageStatus(message4, user1, StatusType.SENT);

        // Given
        UUID userId = user1.getId();

        // When
        ChatDTO chatDTO = chatService.toChatDto(chat1, userId);

        // Then
        assertThat(chatDTO)
                .isNotNull()
                .extracting(ChatDTO::getId, chatDTO1 -> chatDTO1.getUsers().size(), ChatDTO::getLastMessageContent, ChatDTO::getUnreadMessages)
                .containsExactly(chat1.getId(), 2, message4.getContent(), 2);
    }

    @Test
    void toChatEntity_ShouldReturnChatEntity(){
        // Setup
        ChatMemberDTO chatMember1 = ChatMemberDTO.builder().role(ChatterRole.MEMBER).id(UUID.randomUUID()).username("test-user1").build();
        ChatMemberDTO chatMember2 = ChatMemberDTO.builder().role(ChatterRole.CREATOR).id(UUID.randomUUID()).username("test-user2").build();
        ChatMemberDTO chatMember3 = ChatMemberDTO.builder().role(ChatterRole.MEMBER).id(UUID.randomUUID()).username("test-user3").build();

        // Given
        ChatDTO chatDTO = ChatDTO.builder()
                .id(UUID.randomUUID())
                .chatName("Test-chat")
                .users(
                        List.of(chatMember1, chatMember2, chatMember3)
                )
                .chatType(ChatType.GROUP)
                .build();

        // When
        Chat chatEntity = chatService.toChatEntity(chatDTO);

        // Then
        assertThat(chatEntity)
                .isNotNull()
                .extracting(Chat::getId, Chat::getChatName, Chat::getChatType)
                .containsExactlyInAnyOrder(chatDTO.getId(), chatDTO.getChatName(), chatDTO.getChatType());

        // Make sure child chatMemberDTOs where mapped successfully
        assertThat(chatEntity.getUsers())
                .extracting(ChatMember::getId,
                        ChatMember::getRole,
                        chatMember -> chatMember.getUser().getUsername())
                .containsExactlyInAnyOrder(
                        tuple(chatMember1.getId(), chatMember1.getRole(), chatMember1.getUsername()),
                        tuple(chatMember2.getId(), chatMember2.getRole(), chatMember2.getUsername()),
                        tuple(chatMember3.getId(), chatMember3.getRole(), chatMember3.getUsername())
                        );
    }

    @Test
    void saveChat_ShouldReturnSavedChat() {
        // Setup
        User user1 = testDataHelper.saveUser("test-user1");
        User user2 = testDataHelper.saveUser("test-user2");
        ChatMemberDTO chatMember1 = ChatMemberDTO.builder().role(ChatterRole.MEMBER)
                .userId(user1.getId()).username(user1.getUsername()).build();
        ChatMemberDTO chatMember2 = ChatMemberDTO.builder().role(ChatterRole.CREATOR)
                .userId(user2.getId()).username(user2.getUsername()).build();

        // Given
        ChatDTO chatDTO = ChatDTO.builder()
                .users(
                        List.of(chatMember1, chatMember2)
                )
                .chatType(ChatType.REGULAR)
                .build();

        // When
        ChatDTO savedChat = chatService.saveChat(chatDTO);

        // Then
        assertThat(savedChat)
                .isNotNull()
                .extracting(ChatDTO::getChatType)
                .isEqualTo(chatDTO.getChatType());

        assertThat(savedChat.getId()).isNotNull();
    }

    @Test
    void saveChat_ShouldReturnValidationExceptionMoreThan2UsersInRegularChat() {
        // Setup
        ChatMemberDTO chatMember1 = ChatMemberDTO.builder().role(ChatterRole.MEMBER).id(UUID.randomUUID()).username("test-user1").build();
        ChatMemberDTO chatMember2 = ChatMemberDTO.builder().role(ChatterRole.CREATOR).id(UUID.randomUUID()).username("test-user2").build();
        ChatMemberDTO chatMember3 = ChatMemberDTO.builder().role(ChatterRole.MEMBER).id(UUID.randomUUID()).username("test-user3").build();

        // Given
        ChatDTO chatDTO = ChatDTO.builder()
                .users(
                        List.of(chatMember1, chatMember2, chatMember3)
                )
                .chatType(ChatType.REGULAR)
                .build();

        // When / Then
        ValidationException validationException = assertThrows(ValidationException.class, () -> {
            ChatDTO savedChat = chatService.saveChat(chatDTO);
        });

        assertThat(validationException.getMessage()).isEqualTo("A regular chat must contain exactly two users");
    }

    @Test
    void saveChat_ShouldReturnValidationExceptionChatTypeIsRequired() {
        // Setup
        ChatMemberDTO chatMember1 = ChatMemberDTO.builder().role(ChatterRole.MEMBER).id(UUID.randomUUID()).username("test-user1").build();
        ChatMemberDTO chatMember2 = ChatMemberDTO.builder().role(ChatterRole.CREATOR).id(UUID.randomUUID()).username("test-user2").build();

        // Given
        ChatDTO chatDTO = ChatDTO.builder()
                .users(
                        List.of(chatMember1, chatMember2)
                )
                .chatName("Test-chat")
                .build();

        // When / Then
        ValidationException validationException = assertThrows(ValidationException.class, () -> {
            ChatDTO savedChat = chatService.saveChat(chatDTO);
        });

        assertThat(validationException.getMessage()).isEqualTo("Chat type is required");
    }

    @Test
    void saveChat_ShouldReturnValidationExceptionGroupChatNameIsRequired() {
        // Setup
        ChatMemberDTO chatMember1 = ChatMemberDTO.builder().role(ChatterRole.MEMBER).id(UUID.randomUUID()).username("test-user1").build();
        ChatMemberDTO chatMember2 = ChatMemberDTO.builder().role(ChatterRole.CREATOR).id(UUID.randomUUID()).username("test-user2").build();
        ChatMemberDTO chatMember3 = ChatMemberDTO.builder().role(ChatterRole.MEMBER).id(UUID.randomUUID()).username("test-user3").build();

        // Given
        ChatDTO chatDTO = ChatDTO.builder()
                .users(
                        List.of(chatMember1, chatMember2, chatMember3)
                )
                .chatType(ChatType.GROUP)
                .build();

        // When / Then
        ValidationException validationException = assertThrows(ValidationException.class, () -> {
            ChatDTO savedChat = chatService.saveChat(chatDTO);
        });

        assertThat(validationException.getMessage()).isEqualTo("Group chats require a name");
    }

    @Test
    void isChatMember_ShouldReturnTrue() {
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        // Given
        UUID chatId  = groupChat1.getId();
        UUID userId = user4.getId();

        // When
        boolean isMember = chatService.isChatMember(chatId, userId);

        assertThat(isMember).isTrue();
    }

    @Test
    void isChatMember_ShouldThrowNotFoundException() {
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        // Given
        UUID chatId  = UUID.randomUUID();
        UUID userId = user4.getId();

        // When / Then
        assertThrows(NotFoundException.class, () -> {
            boolean isMember = chatService.isChatMember(chatId, userId);
        });
    }

    @Test
    void findChatById_ShouldReturnChatDTO() {
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        Chat groupChat2 = testDataHelper.saveGroupChat();
        ChatMember groupChat2Member1 = testDataHelper.saveChatMember(groupChat2, user1);
        ChatMember groupChat2Member2 = testDataHelper.saveChatMember(groupChat2, user2);
        ChatMember groupChat2Member3 = testDataHelper.saveChatMember(groupChat2, user3);

        Chat regularChat2 = testDataHelper.saveRegularChat();
        testDataHelper.saveChatMember(regularChat2, user1);
        testDataHelper.saveChatMember(regularChat2, user3);

        // Given
        UUID chatId = regularChat1.getId();

        // When
        Optional<ChatDTO> foundChatDTO = chatService.findChatById(chatId);

        // Then
        assertThat(foundChatDTO)
                .isNotEmpty()
                .get()
                .extracting(ChatDTO::getId, ChatDTO::getChatType)
                .containsExactlyInAnyOrder(regularChat1.getId(), regularChat1.getChatType());
    }

    @Test
    void findChatById_ShouldReturnNull() {
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        Chat groupChat2 = testDataHelper.saveGroupChat();
        ChatMember groupChat2Member1 = testDataHelper.saveChatMember(groupChat2, user1);
        ChatMember groupChat2Member2 = testDataHelper.saveChatMember(groupChat2, user2);
        ChatMember groupChat2Member3 = testDataHelper.saveChatMember(groupChat2, user3);

        Chat regularChat2 = testDataHelper.saveRegularChat();
        testDataHelper.saveChatMember(regularChat2, user1);
        testDataHelper.saveChatMember(regularChat2, user3);

        // Given
        UUID chatId = UUID.randomUUID();

        // When / Then
        assertThrows(NotFoundException.class, () -> {
            Optional<ChatDTO> foundChatDTO = chatService.findChatById(chatId);
        });

    }

    @Test
    void findChatEntityById_ShouldReturnChat() {
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        Chat groupChat2 = testDataHelper.saveGroupChat();
        ChatMember groupChat2Member1 = testDataHelper.saveChatMember(groupChat2, user1);
        ChatMember groupChat2Member2 = testDataHelper.saveChatMember(groupChat2, user2);
        ChatMember groupChat2Member3 = testDataHelper.saveChatMember(groupChat2, user3);

        Chat regularChat2 = testDataHelper.saveRegularChat();
        testDataHelper.saveChatMember(regularChat2, user1);
        testDataHelper.saveChatMember(regularChat2, user3);

        // Given
        UUID chatId = regularChat1.getId();

        // When
        Optional<Chat> foundChat = chatService.findChatEntityById(chatId);

        // Then
        assertThat(foundChat)
                .isNotEmpty()
                .get()
                .extracting(Chat::getId, Chat::getChatType)
                .containsExactlyInAnyOrder(regularChat1.getId(), regularChat1.getChatType());
    }

    @Test
    void findChatEntityById_ShouldReturnNull() {
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();

        Chat groupChat1 = testDataHelper.saveGroupChat();
        ChatMember groupChat1Member1 = testDataHelper.saveChatMember(groupChat1, user1);
        ChatMember groupChat1Member2 = testDataHelper.saveChatMember(groupChat1, user2);
        ChatMember groupChat1Member3 = testDataHelper.saveChatMember(groupChat1, user3);
        ChatMember groupChat1Member4 = testDataHelper.saveChatMember(groupChat1, user4);

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        Chat groupChat2 = testDataHelper.saveGroupChat();
        ChatMember groupChat2Member1 = testDataHelper.saveChatMember(groupChat2, user1);
        ChatMember groupChat2Member2 = testDataHelper.saveChatMember(groupChat2, user2);
        ChatMember groupChat2Member3 = testDataHelper.saveChatMember(groupChat2, user3);

        Chat regularChat2 = testDataHelper.saveRegularChat();
        testDataHelper.saveChatMember(regularChat2, user1);
        testDataHelper.saveChatMember(regularChat2, user3);

        // Given
        UUID chatId = UUID.randomUUID();

        // When
        Optional<Chat> foundChatDTO = chatService.findChatEntityById(chatId);

        // Then
        assertThat(foundChatDTO).isEmpty();
    }

}
