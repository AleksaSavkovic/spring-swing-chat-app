package org.chatapp.server.integration.service;

import org.chatapp.common.model.MessageStatusChangeDTO;
import org.chatapp.common.model.MessageStatusDTO;
import org.chatapp.common.model.StatusType;
import org.chatapp.server.entities.*;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.service.MessageStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import({MyTestConfiguration.class, TestDataHelper.class})
public class MessageStatusServiceIT {

    @Autowired
    MessageStatusService messageStatusService;
    @Autowired
    TestDataHelper testDataHelper;

    @Transactional
    @Test
    void updateMessageStatus_ShouldReturnTrue(){
        // Setup
        Chat chat = testDataHelper.saveRegularChat();
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat, user2);

        Message message = testDataHelper.saveMessage(user1, chat);
        MessageStatus messageStatus = testDataHelper.saveMessageStatus(message, user2, StatusType.SENT);

        // Given
        MessageStatusChangeDTO updatedStatus = MessageStatusChangeDTO.builder()
                .messageId(message.getId())
                .userId(user2.getId())
                .status(StatusType.DELIVERED)
                .build();

        // When
        boolean updated = messageStatusService.updateMessageStatus(updatedStatus);

        // Then
        assertThat(updated).isTrue();
    }

    @Transactional
    @Test
    void updateMessageStatus_ShouldThrowException(){
        // Setup
        Chat chat = testDataHelper.saveRegularChat();
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat, user2);

        Message message = testDataHelper.saveMessage(user1, chat);
        MessageStatus messageStatus = testDataHelper.saveMessageStatus(message, user1, StatusType.SENT);

        // Given
        MessageStatusChangeDTO updatedStatus = MessageStatusChangeDTO.builder()
                .messageId(message.getId())
                .userId(user2.getId())
                .status(StatusType.DELIVERED)
                .build();

        // When / Then
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            messageStatusService.updateMessageStatus(updatedStatus);
        });

        // Then
        assertThat(e.getMessage()).isEqualTo("MessageStatus not found or inconsistent for message " + updatedStatus.getMessageId());
    }

    @Transactional
    @Test
    void createMessageStatus_ShouldReturnCreatedStatus() {
        // Setup
        Chat chat = testDataHelper.saveRegularChat();
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat, user2);

        Message message = testDataHelper.saveMessage(user1, chat);

        // Given
        UUID userId = user2.getId();
        UUID messageId = message.getId();

        // When
        MessageStatusDTO newStatus = messageStatusService.createMessageStatus(userId, messageId);

        // Then
        assertThat(newStatus)
                .isNotNull()
                .extracting(MessageStatusDTO::getUserId, MessageStatusDTO::getStatus)
                .containsExactly(userId, StatusType.SENT);

        assertThat(newStatus.getId()).isNotNull();

    }
}
