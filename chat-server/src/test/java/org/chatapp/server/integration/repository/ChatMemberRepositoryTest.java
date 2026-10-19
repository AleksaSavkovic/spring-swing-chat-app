package org.chatapp.server.integration.repository;

import jakarta.persistence.EntityManager;
import org.chatapp.server.entities.*;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.repositories.ChatMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestDataHelper.class, MyTestConfiguration.class})
@ActiveProfiles("test")
public class ChatMemberRepositoryTest {

    @Autowired
    ChatMemberRepository chatMemberRepository;
    @Autowired
    TestDataHelper testDataHelper;
    @Autowired
    EntityManager entityManager;

    @Test
    void findUsersByChatId_Returns2Users() {
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

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID chatId = chat1.getId();

        // When
        List<User> unreadCount = chatMemberRepository.findUsersByChatId(chatId);

        // Then
        assertThat(unreadCount)
                .isNotEmpty()
                .extracting(User::getId)
                .containsExactlyInAnyOrder(
                  chat1Member1.getUser().getId(), chat1Member2.getUser().getId()
                );
    }

    @Test
    void findUsersByChatId_Returns4Users() {
        // Setup
        Chat chat1 = testDataHelper.saveGroupChat();
        Chat chat2 = testDataHelper.saveRegularChat();

        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();
        User user4 = testDataHelper.saveUser();
        User user5 = testDataHelper.saveUser();


        ChatMember chat1Member1 = testDataHelper.saveChatMember(chat1, user1);
        ChatMember chat1Member2 = testDataHelper.saveChatMember(chat1, user2);
        ChatMember chat1Member4 = testDataHelper.saveChatMember(chat1, user4);
        ChatMember chat1Member5 = testDataHelper.saveChatMember(chat1, user5);
        ChatMember chat2Member1 = testDataHelper.saveChatMember(chat2, user1);
        ChatMember chat2Member3 = testDataHelper.saveChatMember(chat2, user3);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID chatId = chat1.getId();

        // When
        List<User> unreadCount = chatMemberRepository.findUsersByChatId(chatId);

        // Then
        assertThat(unreadCount)
                .isNotEmpty()
                .extracting(User::getId)
                .containsExactlyInAnyOrder(
                        chat1Member1.getUser().getId(),
                        chat1Member2.getUser().getId(),
                        chat1Member4.getUser().getId(),
                        chat1Member5.getUser().getId()
                        );
    }

    @Test
    void findUsersByChatId_ReturnsEmptyList() {
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

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID chatId = UUID.randomUUID();

        // When
        List<User> unreadCount = chatMemberRepository.findUsersByChatId(chatId);

        // Then
        assertThat(unreadCount)
                .isEmpty();
    }
}
