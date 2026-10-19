package org.chatapp.server.repositories;

import org.chatapp.server.entities.ChatMember;
import org.chatapp.server.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMemberRepository extends JpaRepository <ChatMember, UUID> {

    @Query("""
        SELECT cm.user
        FROM ChatMember cm
        WHERE cm.chat.id = :chatId
    """)
    List<User> findUsersByChatId(@Param("chatId") UUID chatId);
}
