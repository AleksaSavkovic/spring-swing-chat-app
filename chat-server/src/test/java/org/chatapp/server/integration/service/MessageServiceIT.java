package org.chatapp.server.integration.service;

import jakarta.persistence.EntityManager;
import org.chatapp.common.model.MessageDTO;
import org.chatapp.common.model.MessageType;
import org.chatapp.common.model.UserDTO;
import org.chatapp.server.exceptions.ResourceForbiddenException;
import org.chatapp.server.exceptions.ValidationException;
import org.chatapp.server.entities.*;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import({MyTestConfiguration.class, TestDataHelper.class})
public class MessageServiceIT {
    @Autowired
    MessageService messageService;
    @Autowired
    TestDataHelper testDataHelper;
    @Autowired
    EntityManager entityManager;

    @Transactional
    @Test
    void getChatMessages_ShouldReturnDefaultSizeAndNumberMessagePage() {
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

        List<Message> user1Chat1Messages = testDataHelper.saveMessagesFromSender(chat1, user1, 30);
        List<Message> user2Chat1Messages = testDataHelper.saveMessagesFromSender(chat1, user2, 30);
        List<Message> user1Chat2Messages = testDataHelper.saveMessagesFromSender(chat2, user1, 30);
        List<Message> user3Chat2Messages = testDataHelper.saveMessagesFromSender(chat2, user3, 30);

        entityManager.flush();
        entityManager.clear();
        // Given
        UUID chatId = chat1.getId();
        UUID activeUser = user1.getId();

        // When
        Page<MessageDTO> resultPage = messageService.getChatMessages(chatId, null, null, activeUser);

        // Then
        List<String> resultingMessagesContent = new ArrayList<>();
        assertThat(resultPage)
                .isNotEmpty()
                .hasSize(25);

        // Testing if we got the accurately sorted data and page number
        List<UUID> first25Messages = user2Chat1Messages.stream()
                .sorted(Comparator.comparing(Message::getCreatedDate).reversed())
                .map(Message::getId)
                .limit(25)
                .toList();

        assertThat(resultPage.getContent())
                .extracting(MessageDTO::getId)
                .containsExactlyElementsOf(first25Messages);
    }

    @Transactional
    @Test
    void getChatMessages_ShouldReturnCustomSizeAndNumberMessagePage() {
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

        List<Message> user1Chat1Messages = testDataHelper.saveMessagesFromSender(chat1, user1, 30);
        List<Message> user2Chat1Messages = testDataHelper.saveMessagesFromSender(chat1, user2, 30);
        List<Message> user1Chat2Messages = testDataHelper.saveMessagesFromSender(chat2, user1, 30);
        List<Message> user3Chat2Messages = testDataHelper.saveMessagesFromSender(chat2, user3, 30);

        entityManager.flush();
        entityManager.clear();
        // Given
        UUID chatId = chat1.getId();
        UUID activeUser = user1.getId();

        // When
        Page<MessageDTO> resultPage = messageService.getChatMessages(chatId, 2, 30, activeUser);

        // Then
        List<String> resultingMessagesContent = new ArrayList<>();
        assertThat(resultPage)
                .isNotEmpty()
                .hasSize(30);

        // Testing if we got the accurately sorted data and page number
        assertThat(resultPage.getContent())
                .extracting(MessageDTO::getId)
                .containsExactlyElementsOf(user1Chat1Messages.stream()
                        .sorted(Comparator.comparing(Message::getCreatedDate).reversed())
                        .map(Message::getId).toList());
    }

    @Transactional
    @Test
    void getChatMessages_ShouldThrowForbiddenException() {
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

        // Given
        UUID chatId = chat1.getId();
        UUID activeUser = user3.getId();

        // When / Then
        assertThrows(ResourceForbiddenException.class, () -> {
            Page<MessageDTO> resultPage = messageService.getChatMessages(chatId, null, null, activeUser);
        });
    }

    @Transactional
    @Test
    void saveMessage_ShouldReturnSavedMessage(){
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);

        // Given
        MessageDTO messageDTO = MessageDTO.builder()
                .sender(UserDTO.builder().id(user1.getId()).username(user1.getUsername()).build())
                .chatId(chat1.getId())
                .content("Test message")
                .messageType(MessageType.CHAT_MESSAGE)
                .build();

        // When
        MessageDTO savedMessage = messageService.saveMessage(messageDTO);

        // Then
        assertThat(savedMessage)
                .isNotNull()
                .extracting(
                        MessageDTO::getChatId,
                        MessageDTO::getContent,
                        MessageDTO::getMessageType,
                        messageDTO1 -> messageDTO1.getSender().getId())
                .containsExactly(
                        messageDTO.getChatId(),
                        messageDTO.getContent(),
                        messageDTO.getMessageType(),
                        messageDTO.getSender().getId()
                );
        assertThat(savedMessage.getId()).isNotNull();
    }

    @Transactional
    @Test
    void saveMessage_ShouldReturnNullNoContent(){
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);

        // Given
        MessageDTO messageDTO = MessageDTO.builder()
                .sender(UserDTO.builder().id(user1.getId()).username(user1.getUsername()).build())
                .chatId(chat1.getId())
                .messageType(MessageType.CHAT_MESSAGE)
                .build();

        // When / Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            MessageDTO savedMessage = messageService.saveMessage(messageDTO);
        });

        // Then
        assertThat(exception.getMessage()).isEqualTo("Message content is required");
    }

    @Transactional
    @Test
    void saveMessage_ShouldReturnNullNoChatId(){
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);

        // Given
        MessageDTO messageDTO = MessageDTO.builder()
                .sender(UserDTO.builder().id(user1.getId()).username(user1.getUsername()).build())
                .content("Test message")
                .messageType(MessageType.CHAT_MESSAGE)
                .build();

        // When / Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            MessageDTO savedMessage = messageService.saveMessage(messageDTO);
        });

        // Then
        assertThat(exception.getMessage()).isEqualTo("Message chat id is required");
    }

    @Transactional
    @Test
    void saveMessage_ShouldReturnNullNoSender(){
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);

        // Given
        MessageDTO messageDTO = MessageDTO.builder()
                .content("Test message")
                .chatId(chat1.getId())
                .messageType(MessageType.CHAT_MESSAGE)
                .build();

        // When / Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            MessageDTO savedMessage = messageService.saveMessage(messageDTO);
        });

        // Then
        assertThat(exception.getMessage()).isEqualTo("Message sender is required");
    }

    @Transactional
    @Test
    void toMessageDto_ShouldReturnDto(){
        // Setup
        Chat chat1 = testDataHelper.saveRegularChat();
        User user1 = testDataHelper.saveUser();

        // Given
        Message message = testDataHelper.saveMessage(user1, chat1);

        // When
        MessageDTO messageDTO = messageService.toMessageDto(message);

        // Then
        assertThat(messageDTO)
                .isNotNull()
                .extracting(
                        MessageDTO::getId,
                        messageDTO1 -> messageDTO1.getSender().getId(),
                        MessageDTO::getChatId,
                        MessageDTO::getContent
                ).containsExactly(
                        message.getId(),
                        message.getSender().getId(),
                        message.getChat().getId(),
                        message.getContent()
                );
        // Checking that all message statuses were converted successfully
        assertThat(messageDTO.getStatuses())
                .hasSize(message.getMessageStatuses().size());
    }

//    @Transactional
//    @Test
//    void setChatSeenStatuses_ShouldUpdateStatuses() {
//        // Setup
//        Chat chat1 = testDataHelper.saveRegularChat();
//        Chat chat2 = testDataHelper.saveRegularChat();
//
//        User user1 = testDataHelper.saveUser();
//        User user2 = testDataHelper.saveUser();
//        User user3 = testDataHelper.saveUser();
//
//        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
//        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);
//        ChatMember chat2Member1 = testDataHelper.saveChatMember(chat2, user1);
//        ChatMember chat2Member3 = testDataHelper.saveChatMember(chat2, user3);
//
//        List<Message> user1Chat1Messages = testDataHelper.saveMessagesFromSenderWithStatuses(chat1, user1, 30);
//        List<Message> user2Chat1Messages = testDataHelper.saveMessagesFromSenderWithStatuses(chat1, user2, 30);
//        List<Message> user1Chat2Messages = testDataHelper.saveMessagesFromSenderWithStatuses(chat2, user1, 30);
//        List<Message> user3Chat2Messages = testDataHelper.saveMessagesFromSenderWithStatuses(chat2, user3, 30);
//
//        // Given
//        UUID chatId = chat1.getId();
//        UUID userId = user1.getId();
//
//        // When
//        messageService.setChatSeenStatuses(chatId, userId);
//
//        // Then
//
//
//    }

//    @Test
//    void setBulkDeliveredStatus_ShouldUpdateStatuses() {
//
//    }
//
//    @Test
//    void findMessageById_ShouldReturnMessageDTO() {
//
//    }
//
//    @Test
//    void findMessageById_ShouldReturnNull() {
//
//    }
//
//    @Test
//    void mapDeliveredStatusRecipients_ShouldReturnMap() {
//
//    }

}
