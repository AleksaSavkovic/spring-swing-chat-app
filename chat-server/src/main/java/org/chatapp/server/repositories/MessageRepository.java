package org.chatapp.server.repositories;

import org.chatapp.server.entities.Chat;
import org.chatapp.server.entities.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Optional<Message> findTopByChatOrderByCreatedDateDesc(Chat chat);

    Page<Message> findAllByChatId(UUID chatId, Pageable pageable);

    @Query("""
        SELECT DISTINCT m FROM Message m
        JOIN FETCH m.messageStatuses ms
        WHERE (m.sender.id = :userId OR ms.user.id = :userId)
        AND
        (m.createdDate > :lastSync OR m.updateDate > :lastSync)
        ORDER BY m.createdDate ASC
     """)
    List<Message> syncUserMessagesSince(@Param("userId") UUID userId, @Param("lastSync") Instant lastSync);

    @Modifying
    @Query("""
        UPDATE Message m
        SET m.active = false,
            m.updateDate = :updateDate
        WHERE m.id = :messageId
     """
    )
    void setInactiveById(@Param("messageId") UUID messageId, @Param("updateDate")Instant updateDate);

    List<Message> findByIdIn(Collection<UUID> ids);

}
