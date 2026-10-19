package org.chatapp.server.integration.repository;

import jakarta.persistence.EntityManager;
import org.chatapp.server.entities.Chat;
import org.chatapp.server.entities.ChatMember;
import org.chatapp.server.entities.User;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.repositories.ChatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestDataHelper.class, MyTestConfiguration.class})
@ActiveProfiles("test")
public class ChatRepositoryTest {

    @Autowired
    ChatRepository chatRepository;

    @Autowired
    TestDataHelper testDataHelper;

    @Autowired
    EntityManager entityManager;

    @Test
    void syncUserChatsSince_ShouldReturnChatsAfterLastSyncWithUser() {
        // Setup
        User activeUser = testDataHelper.saveUser();
        User randomUser = testDataHelper.saveUser();

        Chat chatBefore = testDataHelper.saveChatWithTimeStamps(
                Instant.parse("2026-06-22T12:00:00Z"),
                Instant.parse("2026-06-22T12:00:00Z"));
        chatBefore.addUsers(Set.of(testDataHelper.saveChatMember(chatBefore, activeUser)));

        Chat chatAfter = testDataHelper.saveChatWithTimeStamps(
                Instant.parse("2026-06-26T12:00:00Z"),
                Instant.parse("2026-06-26T12:00:00Z"));
        chatAfter.addUsers(Set.of(testDataHelper.saveChatMember(chatAfter, activeUser)));

        Chat chatOnlyUpdateAfter = testDataHelper.saveChatWithTimeStamps(
                Instant.parse("2026-06-26T12:00:00Z"),
                Instant.parse("2026-06-26T12:00:00Z"));
        chatOnlyUpdateAfter.addUsers(Set.of(testDataHelper.saveChatMember(chatOnlyUpdateAfter, activeUser)));

        Chat chatAfterButNoUser = testDataHelper.saveChatWithTimeStamps(
                Instant.parse("2026-06-26T12:00:00Z"),
                Instant.parse("2026-06-26T12:00:00Z"));
        chatAfterButNoUser.addUsers(Set.of(testDataHelper.saveChatMember(chatAfterButNoUser, randomUser)));

        // Given
        Instant lastSync = Instant.parse("2026-06-26T12:00:00Z");
        UUID userId = activeUser.getId();

        // When
        List<Chat> result = chatRepository.syncUserChatsSince(userId, lastSync);

        // Then
        assertThat(result)
                .hasSize(3)
                .contains(chatBefore, chatAfter, chatOnlyUpdateAfter);
    }

    @Test
    void findChatsByIdsWithUsers_ShouldReturnChatEntityWithListOfChatMembers() {
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

        entityManager.flush();
        entityManager.clear();

        // Given
        List<UUID> chatIds = List.of(
                groupChat1.getId(),
                regularChat1.getId(),
                groupChat2.getId()
        );

        // When
        Set<Chat> result = chatRepository.findChatsByIdsWithUsers(chatIds);

        // Then
        assertThat(result)
                .hasSize(3)
                .extracting(Chat::getId)
                .containsExactlyInAnyOrder(
                        groupChat1.getId(),
                        regularChat1.getId(),
                        groupChat2.getId()
                );

        // Check groupChat1 saved users
        Chat fetchedGroupChat1 = result.stream()
                .filter(chat -> chat.getId().equals(groupChat1.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(fetchedGroupChat1.getUsers())
                .extracting(ChatMember::getId)
                .containsExactlyInAnyOrder(
                        groupChat1Member1.getId(),
                        groupChat1Member2.getId(),
                        groupChat1Member3.getId(),
                        groupChat1Member4.getId()
                );

        // Check regularChat1 saved users
        Chat fetchedRegularChat1 = result.stream()
                .filter(chat -> chat.getId().equals(regularChat1.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(fetchedRegularChat1.getUsers())
                .extracting(ChatMember::getId)
                .containsExactlyInAnyOrder(
                        regularChat1Member1.getId(),
                        regularChat1Member2.getId()
                );

        // Check groupChat2 saved users
        Chat fetchedGroupChat2 = result.stream()
                .filter(chat -> chat.getId().equals(groupChat2.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(fetchedGroupChat2.getUsers())
                .extracting(ChatMember::getId)
                .containsExactlyInAnyOrder(
                        groupChat2Member1.getId(),
                        groupChat2Member2.getId(),
                        groupChat2Member3.getId()
                );

        // Make sure that the result does not contain regularChat2
        assertThat(result)
                .extracting(Chat::getId)
                .doesNotContain(regularChat2.getId());
    }

    @Test
    void findChatWithUsers_ShouldReturnChatWithUsers(){
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        // This chat should NOT be returned
        Chat regularChat2 = testDataHelper.saveRegularChat();
        testDataHelper.saveChatMember(regularChat2, user1);
        testDataHelper.saveChatMember(regularChat2, user3);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID user1Id = user1.getId();
        UUID user2Id = user2.getId();

        // When
        Optional<Chat> foundChat = chatRepository.findChatWithUsers(user1Id, user2Id);

        // Then
        assertThat(foundChat)
                .isNotEmpty()
                .get()
                .extracting(Chat::getId)
                .isEqualTo(regularChat1.getId());

        // Check if the fetched chat has the given users
        Chat foundChatValue = foundChat.get();
        assertThat(foundChatValue.getUsers())
                .extracting(ChatMember::getId)
                .containsExactlyInAnyOrder(
                        regularChat1Member1.getId(),
                        regularChat1Member2.getId()
                );
    }

    @Test
    void findChatWithUsers_ShouldReturnEmptyOptional(){
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();
        User user3 = testDataHelper.saveUser();

        Chat regularChat1 = testDataHelper.saveRegularChat();
        ChatMember regularChat1Member1 = testDataHelper.saveChatMember(regularChat1, user1);
        ChatMember regularChat1Member2 = testDataHelper.saveChatMember(regularChat1, user2);

        // This chat should NOT be returned
        Chat regularChat2 = testDataHelper.saveRegularChat();
        testDataHelper.saveChatMember(regularChat2, user1);
        testDataHelper.saveChatMember(regularChat2, user3);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID user2Id = user2.getId();
        UUID user3Id = user3.getId();

        // When
        Optional<Chat> foundChat = chatRepository.findChatWithUsers(user2Id, user3Id);

        // Then
        assertThat(foundChat)
                .isEmpty();
    }
}
