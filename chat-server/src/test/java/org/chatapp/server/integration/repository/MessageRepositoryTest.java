package org.chatapp.server.integration.repository;

import jakarta.persistence.EntityManager;
import org.chatapp.server.entities.*;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.repositories.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;


import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@Import({TestDataHelper.class, MyTestConfiguration.class})
@ActiveProfiles("test")
public class MessageRepositoryTest {

    @Autowired
    MessageRepository messageRepository;
    @Autowired
    TestDataHelper testDataHelper;
    @Autowired
    EntityManager entityManager;

    @Test
    void findTopByChatOrderByCreatedDateDesc_ShouldReturnMessage() {
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

        entityManager.flush();
        entityManager.clear();

        // Given
        Chat chat = chat1;

        // When
        Optional<Message> topMessage = messageRepository.findTopByChatOrderByCreatedDateDesc(chat);

        // Then
        assertThat(topMessage)
                .isNotEmpty()
                .get()
                .extracting(Message::getId)
                .isEqualTo(message4.getId());
    }

    @Test
    void findAllByChatId_ShouldReturnPageWith4Elements() {
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

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID chatId = chat1.getId();
        PageRequest pageRequest = PageRequest.of(0, 25, Sort.by(Sort.Order.desc("createdDate")));

        // When
        Page<Message> resultPage = messageRepository.findAllByChatId(chatId, pageRequest);

        // Then
        assertThat(resultPage)
                .isNotEmpty()
                .hasSize(4)
                .extracting(Message::getId, Message::getContent)
                .containsExactly(
                        tuple(message4.getId(), message4.getContent()),
                        tuple(message3.getId(), message3.getContent()),
                        tuple(message2.getId(), message2.getContent()),
                        tuple(message1.getId(), message1.getContent()));
    }

    @Test
    void findAllByChatId_ShouldReturnEmptyPage() {
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

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID chatId = chat2.getId();
        PageRequest pageRequest = PageRequest.of(0, 25, Sort.by(Sort.Order.desc("createdDate")));

        // When
        Page<Message> resultPage = messageRepository.findAllByChatId(chatId, pageRequest);

        // Then
        assertThat(resultPage)
                .isEmpty();
    }

}
