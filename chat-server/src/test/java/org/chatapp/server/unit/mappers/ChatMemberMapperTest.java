package org.chatapp.server.unit.mappers;

import lombok.extern.slf4j.Slf4j;
import org.chatapp.common.model.ChatMemberDTO;
import org.chatapp.common.model.ChatType;
import org.chatapp.common.model.ChatterRole;
import org.chatapp.server.entities.*;
import org.chatapp.server.mappers.ChatMemberMapper;
import org.chatapp.server.mappers.ChatMemberMapperImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ChatMemberMapperImpl.class })
class ChatMemberMapperTest {

    @Autowired
    private ChatMemberMapper chatMemberMapper;

    private static Chat chat;
    private static User user;

    @BeforeAll
    static void setUp() {
        chat = Chat.builder()
                .id(UUID.randomUUID())
                .chatType(ChatType.REGULAR)
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .username("Marko Markovic")
                .password("61a862f3ad24850772d9d660e1f21dfa25d7eca0390d9a42c92ffd4e820a6d9d")
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();
    }

    @Test
    void testToDto() {
        Instant muted = Instant.now();

        ChatMember entity = ChatMember.builder()
                .id(UUID.randomUUID())
                .chat(chat)
                .user(user)
                .mutedUntil(muted)
                .pinned(true)
                .blocked(true)
                .role(ChatterRole.MEMBER)
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();

        ChatMemberDTO dto = chatMemberMapper.toDto(entity);
        assertThat(dto.getId()).isEqualTo(entity.getId());
        assertThat(dto.isBlocked()).isEqualTo(true);
        assertThat(dto.isPinned()).isEqualTo(true);
        assertThat(dto.getRole()).isEqualTo(org.chatapp.common.model.ChatterRole.MEMBER);
        assertThat(dto.getUsername()).isEqualTo(entity.getUser().getUsername());
        assertThat(dto.getUserId()).isEqualTo(entity.getUser().getId());
//        assertThat(dto.getCreatedDate()).isEqualTo(entity.getCreatedDate()); Removed these attributes from dto
//        assertThat(dto.getUpdateDate()).isEqualTo(entity.getUpdateDate());

        log.debug(dto.toString());

    }

    @Test
    void testToEntity() {
        Instant muted = Instant.now();

        ChatMemberDTO dto = ChatMemberDTO.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .username(user.getUsername())
                .mutedUntil(muted)
                .pinned(true)
                .blocked(true)
                .role(org.chatapp.common.model.ChatterRole.MEMBER)
//                .createdDate(Instant.now()) Removed these attributes from dto
//                .updateDate(Instant.now())
                .build();

        ChatMember entity = chatMemberMapper.toEntity(dto);
        assertThat(entity.getId()).isEqualTo(dto.getId());
        assertThat(entity.isBlocked()).isEqualTo(true);
        assertThat(entity.isPinned()).isEqualTo(true);
        assertThat(entity.getRole()).isEqualTo(ChatterRole.MEMBER);
        assertThat(entity.getUser().getUsername()).isEqualTo(dto.getUsername());
        assertThat(entity.getUser().getId()).isEqualTo(dto.getUserId());
//        assertThat(entity.getCreatedDate()).isEqualTo(dto.getCreatedDate()); Removed these attributes from dto
//        assertThat(entity.getUpdateDate()).isEqualTo(dto.getUpdateDate());

        log.debug(entity.toString());
    }
}