package org.chatapp.server.repositories;

import org.chatapp.server.entities.MessageStatus;
import org.chatapp.common.model.StatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageStatusRepository extends JpaRepository<MessageStatus, UUID> {

    @Query(
    """
    SELECT COUNT(ms)
    FROM MessageStatus ms
    JOIN ms.message m
    WHERE m.chat.id = :chatId
        AND ms.user.id = :userId
        AND ms.status <> 'SEEN'
    """)
    int countUnreadMessages(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    List<MessageStatus> findByMessageIdIn(List<UUID> messages);

    @Query(
    """
    SELECT ms
    FROM MessageStatus ms
    WHERE ms.message.createdDate >= :firstMsg
        AND ms.message.chat.id = :chatId
        AND (
            :lastSync IS NULL
            OR ms.createdDate > :lastSync
            OR ms.updateDate > :lastSync
            )
    """)
    List<MessageStatus> getNewChatMsgStatuses(@Param("chatId") UUID chatId,
                                              @Param("firstMsg") Instant firstMsg,
                                              @Param("lastSync") Instant lastSync);

    @Modifying
    @Query("""
       UPDATE MessageStatus ms
       SET ms.status = :status
       WHERE ms.message.id = :messageId
           AND ms.user.id = :userId
           AND ms.status <> 'SEEN'
    """)
    int updateByMessageIdAndUserId(@Param("messageId") UUID messageId,
                                   @Param("userId") UUID userId,
                                   @Param("status") StatusType status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE MessageStatus ms
        SET ms.status = :status
        WHERE ms.user.id = :userId
            AND ms.message.chat.id = :chatId
            AND ms.status <> :status
    """)
    int updateChatMessagesStatus(@Param("chatId") UUID chatId,
                                 @Param("userId") UUID userId,
                                 @Param("status") StatusType status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE MessageStatus ms
        SET ms.status = :status
        WHERE ms.message.id IN :messageList
            AND ms.user.id = :userId
            AND ms.status <> 'SEEN'
     """)
    int updateByMessageListAndUserId(@Param("userId") UUID userId,
                                     @Param("messageList") List<UUID> messageList,
                                     @Param("status") StatusType status);
}
