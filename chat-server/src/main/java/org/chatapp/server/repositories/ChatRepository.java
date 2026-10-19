package org.chatapp.server.repositories;

import org.chatapp.server.entities.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    List<Chat> findAllByUsersUserId(UUID userId);


    @Query("""
        SELECT DISTINCT c FROM Chat c
        JOIN FETCH c.users cm
        WHERE c.id IN (
            SELECT c2.id
            FROM Chat c2
            JOIN c2.users currentUserMember
            WHERE currentUserMember.user.id = :userId
        )
        AND
        (c.createdDate > :lastSync OR c.updateDate > :lastSync
        OR cm.createdDate > :lastSync OR cm.updateDate > :lastSync)
     """)
    List<Chat> syncUserChatsSince(@Param("userId") UUID userId, @Param("lastSync") Instant lastSync);

    @Query("""
        SELECT DISTINCT c FROM Chat c
        JOIN FETCH c.users cm
        WHERE c.id IN (:chatIds)
    """)
    Set<Chat> findChatsByIdsWithUsers(@Param("chatIds") List<UUID> chatIds);

    @Query("""
           SELECT c FROM Chat c
           JOIN c.users u1
           JOIN c.users u2
           WHERE c.chatType = 'REGULAR'
           AND u1.user.id = :user1Id
           AND u2.user.id = :user2Id
           """)
    Optional<Chat> findChatWithUsers(@Param("user1Id") UUID user1Id, @Param("user2Id") UUID user2Id);


}
