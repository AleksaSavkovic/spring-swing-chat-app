package org.chatapp.server.unit.mappers;

import lombok.extern.slf4j.Slf4j;
import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.ChatMemberDTO;
import org.chatapp.common.model.ChatType;
import org.chatapp.common.model.ChatterRole;
import org.chatapp.server.entities.*;
import org.chatapp.server.mappers.ChatMapper;
import org.chatapp.server.mappers.ChatMapperImpl;
import org.chatapp.server.mappers.ChatMemberMapper;
import org.chatapp.server.mappers.ChatMemberMapperImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ChatMapperImpl.class, ChatMemberMapperImpl.class})
class ChatMapperTest {

    @Autowired
    ChatMapper chatMapper;

    @Autowired
    ChatMemberMapper chatMemberMapper;

    Set<ChatMember> addChatMembers(Chat chat) {

        // Create users
        User user1 = User.builder()
                .id(UUID.randomUUID())
                .username("Marko Markovic")
                .password("61a862f3ad24850772d9d660e1f21dfa25d7eca0390d9a42c92ffd4e820a6d9d")
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();
        User user2 = User.builder()
                .id(UUID.randomUUID())
                .username("Janko Jankovic")
                .password("61a862f3ad24850772d9d660e1f21dfa25d7eca0390d9a42c92ffd4e820a6d9d")
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();
        User user3 = User.builder()
                .id(UUID.randomUUID())
                .username("Petar Perovic")
                .password("61a862f3ad24850772d9d660e1f21dfa25d7eca0390d9a42c92ffd4e820a6d9d")
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();


        ChatMember chatMember1 = ChatMember.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .chat(chat)
                .role(ChatterRole.MEMBER)
                .mutedUntil(null)
                .pinned(false)
                .blocked(false)
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();

        ChatMember chatMember2 = ChatMember.builder()
                .id(UUID.randomUUID())
                .user(user2)
                .chat(chat)
                .role(ChatterRole.ADMIN)
                .mutedUntil(null)
                .pinned(false)
                .blocked(false)
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();

        ChatMember chatMember3 = ChatMember.builder()
                .id(UUID.randomUUID())
                .user(user3)
                .chat(chat)
                .role(ChatterRole.MEMBER)
                .mutedUntil(null)
                .pinned(true)
                .blocked(true)
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();

        Set<ChatMember> members = new HashSet<>();
        members.add(chatMember1);
        members.add(chatMember2);
        members.add(chatMember3);

        return members;

    }

    List<ChatMemberDTO> addChatDTOMembers(ChatDTO chat) {
        ChatMemberDTO chatMember1 = ChatMemberDTO.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .username("Marko Markovic")
                .role(org.chatapp.common.model.ChatterRole.MEMBER)
                .mutedUntil(null)
                .pinned(false)
                .blocked(false)
                .build();

        ChatMemberDTO chatMember2 = ChatMemberDTO.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .username("Janko Jankovic")
                .role(org.chatapp.common.model.ChatterRole.ADMIN)
                .mutedUntil(null)
                .pinned(false)
                .blocked(false)
                .build();

        ChatMemberDTO chatMember3 = ChatMemberDTO.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .username("Petar Perovic")
                .role(org.chatapp.common.model.ChatterRole.MEMBER)
                .mutedUntil(null)
                .pinned(true)
                .blocked(true)
                .build();

        List <ChatMemberDTO> members = new ArrayList<>();
        members.add(chatMember1);
        members.add(chatMember2);
        members.add(chatMember3);

        return members;
    }

    @Test
    void testToDto() {
        Chat entity = Chat.builder()
                .id(UUID.randomUUID())
                .chatType(ChatType.GROUP)
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .chatName("Testna grupa")
                .password("90d9a42c92ffd4e820a6d99d660e1f21dfa25d7eca0390d9a42c92ffd4e820a6d9d")
                .build();
        entity.setUsers(addChatMembers(entity));

        ChatDTO dto = chatMapper.toDto(entity);

        log.debug(dto.toString());

        assertThat(dto.getId()).isEqualTo(entity.getId());
        assertThat(dto.getUsers()).isNotEmpty();
        assertThat(dto.getChatType()).isEqualTo(org.chatapp.common.model.ChatType.GROUP);
        assertThat(dto.getChatName()).isEqualTo(entity.getChatName());

    }

    @Test
    void testToEntity() {
        ChatDTO dto = ChatDTO.builder()
                .id(UUID.randomUUID())
                .chatType(ChatType.GROUP)
                .chatName("Testna grupa")
                .build();

        dto.setUsers(addChatDTOMembers(dto));


        Chat entity = chatMapper.toEntity(dto);

        log.debug(entity.toString());

        assertThat(entity.getId()).isEqualTo(dto.getId());
        // This was changed now the users are not converted by the mapper but manually with a service helper method
        assertThat(entity.getChatType()).isEqualTo(ChatType.GROUP);
        assertThat(entity.getChatName()).isEqualTo(dto.getChatName());
    }
}