package org.chatapp.server.integration.repository;

import jakarta.persistence.EntityManager;
import org.chatapp.common.model.StatusType;
import org.chatapp.server.entities.*;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.repositories.MessageStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@Import({TestDataHelper.class, MyTestConfiguration.class})
@ActiveProfiles("test")
public class MessageStatusRepositoryTest {

    @Autowired
    MessageStatusRepository messageStatusRepository;
    @Autowired
    TestDataHelper testDataHelper;
    @Autowired
    EntityManager entityManager;

    @Test
    void countUnreadMessages_ShouldReturn2(){
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();
        Chat chat2 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);
        ChatMember chat2Member1 = testDataHelper.saveChatMember(chat2, user1);
        ChatMember chat2Member3 = testDataHelper.saveChatMember(chat2, user3);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user1, chat1);
        Message message3 = testDataHelper.saveMessage(user2, chat1);
        Message message4 = testDataHelper.saveMessage(user2, chat1);
        Message message5 = testDataHelper.saveMessage(user3, chat2);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.DELIVERED);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user2, StatusType.SEEN);
        MessageStatus messageStatus3 = testDataHelper.saveMessageStatus(message3, user1, StatusType.SEEN);
        MessageStatus messageStatus4 = testDataHelper.saveMessageStatus(message4, user1, StatusType.DELIVERED);
        MessageStatus messageStatus5 = testDataHelper.saveMessageStatus(message5, user1, StatusType.DELIVERED);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID chatId = chat1.getId();
        UUID userId = user1.getId();

        // When
        int unreadCount = messageStatusRepository.countUnreadMessages(chatId, userId);

        // Then
        assertThat(unreadCount).isEqualTo(2);
    }

    @Test
    void countUnreadMessages_ShouldReturn0(){
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user1, chat1);
        Message message3 = testDataHelper.saveMessage(user2, chat1);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.SEEN);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user2, StatusType.SEEN);
        MessageStatus messageStatus3 = testDataHelper.saveMessageStatus(message3, user1, StatusType.SEEN);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID chatId = chat1.getId();
        UUID userId = user1.getId();

        // When
        int unreadCount = messageStatusRepository.countUnreadMessages(chatId, userId);

        // Then
        assertThat(unreadCount).isEqualTo(0);
    }

    @Test
    void findByMessageIdIn_ShouldReturn3Messages() {
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();
        Chat chat2 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);
        ChatMember chat2Member1 = testDataHelper.saveChatMember(chat2, user1);
        ChatMember chat2Member3 = testDataHelper.saveChatMember(chat2, user3);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user1, chat1);
        Message message3 = testDataHelper.saveMessage(user2, chat1);
        Message message4 = testDataHelper.saveMessage(user2, chat1);
        Message message5 = testDataHelper.saveMessage(user3, chat2);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.DELIVERED);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user2, StatusType.SEEN);
        MessageStatus messageStatus3 = testDataHelper.saveMessageStatus(message3, user1, StatusType.SEEN);
        MessageStatus messageStatus4 = testDataHelper.saveMessageStatus(message4, user1, StatusType.DELIVERED);
        MessageStatus messageStatus5 = testDataHelper.saveMessageStatus(message5, user1, StatusType.DELIVERED);

        entityManager.flush();
        entityManager.clear();

        // Given
        List<UUID> messageIds = List.of(message1.getId(), message2.getId(), message3.getId());

        // When
        List<MessageStatus> foundStatuses = messageStatusRepository.findByMessageIdIn(messageIds);

        // Then
        assertThat(foundStatuses)
                .isNotEmpty()
                .hasSize(3)
                .extracting(MessageStatus::getId)
                .containsExactlyInAnyOrder(messageStatus1.getId(), messageStatus2.getId(), messageStatus3.getId());
    }

    @Test
    void findByMessageIdIn_ShouldReturnEmptyList() {
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();
        Chat chat2 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);
        ChatMember chat2Member1 = testDataHelper.saveChatMember(chat2, user1);
        ChatMember chat2Member3 = testDataHelper.saveChatMember(chat2, user3);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user1, chat1);
        Message message3 = testDataHelper.saveMessage(user2, chat1);
        Message message4 = testDataHelper.saveMessage(user2, chat1);
        Message message5 = testDataHelper.saveMessage(user3, chat2);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.DELIVERED);
        MessageStatus messageStatus4 = testDataHelper.saveMessageStatus(message4, user1, StatusType.DELIVERED);
        MessageStatus messageStatus5 = testDataHelper.saveMessageStatus(message5, user1, StatusType.DELIVERED);

        entityManager.flush();
        entityManager.clear();

        // Given
        List<UUID> messageIds = List.of(message2.getId(), message3.getId());

        // When
        List<MessageStatus> foundStatuses = messageStatusRepository.findByMessageIdIn(messageIds);

        // Then
        assertThat(foundStatuses)
                .isEmpty();
    }

    @Test
    void updateByMessageIdAndUserId_ShouldReturn1() {
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user2, chat1);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.DELIVERED);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user1, StatusType.SENT);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID messageId = message1.getId();
        UUID userId = user1.getId();
        StatusType status = StatusType.SEEN;

        // When
        int updated = messageStatusRepository.updateByMessageIdAndUserId(messageId, userId, status);

        // Then
        assertThat(updated).isEqualTo(1);
        Optional<MessageStatus> updatedMessageStatus = messageStatusRepository.findById(messageStatus1.getId());
        assertThat(updatedMessageStatus)
                .isNotEmpty()
                .get()
                .extracting(MessageStatus::getStatus)
                .isEqualTo(StatusType.SEEN);
    }

    @Test
    void updateByMessageIdAndUserId_ShouldReturn0StatusIsSEEN() {
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user2, chat1);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.SEEN);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user1, StatusType.SENT);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID messageId = message1.getId();
        UUID userId = user1.getId();
        StatusType status = StatusType.SEEN;

        // When
        int updated = messageStatusRepository.updateByMessageIdAndUserId(messageId, userId, status);

        // Then
        assertThat(updated).isEqualTo(0);
        Optional<MessageStatus> notUpdatedMessageStatus = messageStatusRepository.findById(messageStatus1.getId());
        assertThat(notUpdatedMessageStatus)
                .isNotEmpty()
                .get()
                .extracting(MessageStatus::getStatus)
                .isEqualTo(StatusType.SEEN);
    }

    @Test
    void updateByMessageIdAndUserId_ShouldReturn0NoStatus() {
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user2, chat1);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.DELIVERED);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user1, StatusType.SENT);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID messageId = message1.getId();
        UUID userId = user2.getId();
        StatusType status = StatusType.SEEN;

        // When
        int updated = messageStatusRepository.updateByMessageIdAndUserId(messageId, userId, status);

        // Then
        assertThat(updated).isEqualTo(0);
    }

    @Test
    void updateChatMessagesStatus_ShouldReturn2(){
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();
        Chat chat2 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);
        ChatMember chat2Member1 = testDataHelper.saveChatMember(chat2, user1);
        ChatMember chat2Member3 = testDataHelper.saveChatMember(chat2, user3);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user1, chat1);
        Message message3 = testDataHelper.saveMessage(user2, chat1);
        Message message4 = testDataHelper.saveMessage(user2, chat1);
        Message message5 = testDataHelper.saveMessage(user3, chat2);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.DELIVERED);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user2, StatusType.DELIVERED);
        MessageStatus messageStatus3 = testDataHelper.saveMessageStatus(message3, user1, StatusType.SENT);
        MessageStatus messageStatus4 = testDataHelper.saveMessageStatus(message4, user1, StatusType.SENT);
        MessageStatus messageStatus5 = testDataHelper.saveMessageStatus(message5, user1, StatusType.SENT);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID chatId = chat1.getId();
        UUID userId = user1.getId();
        StatusType status = StatusType.DELIVERED;

        // When
        int updatedCount = messageStatusRepository.updateChatMessagesStatus(chatId, userId, status);

        // Then
        assertThat(updatedCount).isEqualTo(2);

        List<MessageStatus> updatedMessagesStatus = messageStatusRepository.findByMessageIdIn(List.of(message3.getId(), message4.getId()));
        assertThat(updatedMessagesStatus)
                .isNotEmpty()
                .extracting(MessageStatus::getId, MessageStatus::getStatus)
                .containsExactlyInAnyOrder(
                        tuple(messageStatus3.getId(), StatusType.DELIVERED),
                        tuple(messageStatus4.getId(), StatusType.DELIVERED)
                );
    }

    @Test
    void updateChatMessagesStatus_ShouldReturn0OnlySameStatus(){
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user1, chat1);
        Message message3 = testDataHelper.saveMessage(user2, chat1);
        Message message4 = testDataHelper.saveMessage(user2, chat1);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.DELIVERED);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user2, StatusType.DELIVERED);
        MessageStatus messageStatus3 = testDataHelper.saveMessageStatus(message3, user1, StatusType.DELIVERED);
        MessageStatus messageStatus4 = testDataHelper.saveMessageStatus(message4, user1, StatusType.DELIVERED);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID chatId = chat1.getId();
        UUID userId = user1.getId();
        StatusType status = StatusType.DELIVERED;

        // When
        int updatedCount = messageStatusRepository.updateChatMessagesStatus(chatId, userId, status);

        // Then
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    void updateByMessageListAndUserId_ShouldReturn3() {
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();
        Chat chat2 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);
        ChatMember chat2Member1 = testDataHelper.saveChatMember(chat2, user1);
        ChatMember chat2Member3 = testDataHelper.saveChatMember(chat2, user3);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user1, chat1);
        Message message3 = testDataHelper.saveMessage(user2, chat1);
        Message message4 = testDataHelper.saveMessage(user2, chat1);
        Message message5 = testDataHelper.saveMessage(user3, chat2);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.DELIVERED);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user2, StatusType.SEEN);
        MessageStatus messageStatus3 = testDataHelper.saveMessageStatus(message3, user1, StatusType.SENT);
        MessageStatus messageStatus4 = testDataHelper.saveMessageStatus(message4, user1, StatusType.SENT);
        MessageStatus messageStatus5 = testDataHelper.saveMessageStatus(message5, user1, StatusType.SENT);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID userId = user1.getId();
        StatusType status = StatusType.DELIVERED;

        // When
        int updatedCount = messageStatusRepository.updateByMessageListAndUserId(
                userId,
                List.of(message3.getId(), message4.getId(), message5.getId()),
                status);

        // Then
        assertThat(updatedCount).isEqualTo(3);

        List<MessageStatus> updatedMessagesStatus = messageStatusRepository
                .findByMessageIdIn(List.of(message3.getId(), message4.getId(), message5.getId()));
        assertThat(updatedMessagesStatus)
                .isNotEmpty()
                .extracting(MessageStatus::getId, MessageStatus::getStatus)
                .containsExactlyInAnyOrder(
                        tuple(messageStatus3.getId(), StatusType.DELIVERED),
                        tuple(messageStatus4.getId(), StatusType.DELIVERED),
                        tuple(messageStatus5.getId(), StatusType.DELIVERED)
                );
    }

    @Test
    void updateByMessageListAndUserId_ShouldReturn0() {
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();
        Chat chat2 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);
        ChatMember chat2Member1 = testDataHelper.saveChatMember(chat2, user1);
        ChatMember chat2Member3 = testDataHelper.saveChatMember(chat2, user3);

        Message message1 = testDataHelper.saveMessage(user2, chat1);
        Message message2 = testDataHelper.saveMessage(user1, chat1);
        Message message3 = testDataHelper.saveMessage(user2, chat1);
        Message message4 = testDataHelper.saveMessage(user2, chat1);
        Message message5 = testDataHelper.saveMessage(user3, chat2);

        MessageStatus messageStatus1 = testDataHelper.saveMessageStatus(message1, user1, StatusType.SENT);
        MessageStatus messageStatus2 = testDataHelper.saveMessageStatus(message2, user2, StatusType.SEEN);
        MessageStatus messageStatus3 = testDataHelper.saveMessageStatus(message3, user1, StatusType.SENT);
        MessageStatus messageStatus4 = testDataHelper.saveMessageStatus(message4, user1, StatusType.SEEN);
        MessageStatus messageStatus5 = testDataHelper.saveMessageStatus(message5, user1, StatusType.SEEN);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID chatId = chat1.getId();
        StatusType status = StatusType.DELIVERED;

        // When
        int updatedCount = messageStatusRepository.updateByMessageListAndUserId(
                chatId,
                List.of(message3.getId(), message4.getId(), message5.getId()),
                status);

        // Then
        assertThat(updatedCount).isEqualTo(0);
    }
}
