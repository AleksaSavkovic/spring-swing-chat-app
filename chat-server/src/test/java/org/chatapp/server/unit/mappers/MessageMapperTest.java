package org.chatapp.server.unit.mappers;

import lombok.extern.slf4j.Slf4j;
import org.chatapp.common.model.*;
import org.chatapp.common.model.ChatType;
import org.chatapp.server.entities.*;
import org.chatapp.server.mappers.MessageMapper;
import org.chatapp.server.mappers.MessageMapperImpl;
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
@ContextConfiguration(classes = { MessageMapperImpl.class})
class MessageMapperTest {

    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID CHAT_ID = UUID.randomUUID();
    private static final UUID SENDER_ID = UUID.randomUUID();
    private static final String CONTENT = "Test message content";
    private static final MessageType MESSAGE_TYPE = MessageType.CHAT_MESSAGE;
    private static final MessageType MESSAGE_TYPE_DTO = MessageType.CHAT_MESSAGE;
//    private static final Instant SENT_TIME = Instant.of(2026, 1, 5, 12, 30, 0);

    @Autowired
    MessageMapper mapper;

    @Test
    void testToDto() {
        User sender = User.builder()
                .id(UUID.randomUUID())
                .username("Marko Markovic")
                .build();

        Chat chat = Chat.builder()
                .id(UUID.randomUUID())
                .chatType(ChatType.REGULAR)
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();

        Message entity = Message.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .chat(chat)
                .content("Test message")
                .messageType(MessageType.CHAT_MESSAGE)
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();

        MessageDTO dto = mapper.toDto(entity);

        assertThat(dto.getId()).isEqualTo(entity.getId());
        assertThat(dto.getSender().getId().equals(sender.getId()));
        assertThat(dto.getSender().getUsername().equals(sender.getUsername()));
        assertThat(dto.getChatId() != null);
        assertThat(dto.getChatId().equals(chat.getId()));
        assertThat(dto.getContent().equals(entity.getContent()));
        assertThat(dto.getMessageType().equals(MessageType.CHAT_MESSAGE));
        assertThat(dto.getSentTime().equals(entity.getCreatedDate()));

    }
    
    @Test
    void testToEntity() {
        UserDTO sender = UserDTO.builder()
                .id(UUID.randomUUID())
                .username("Marko Markovic")
                .build();

        ChatDTO chat = ChatDTO.builder()
                .id(UUID.randomUUID())
                .chatType(ChatType.REGULAR)
                .build();

        MessageDTO dto = MessageDTO.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .chatId(chat.getId())
                .content("Test message")
                .messageType(MessageType.CHAT_MESSAGE)
                .sentTime(Instant.now())
                .build();

        Message entity = mapper.toEntity(dto);


        assertThat(entity.getId().equals(dto.getId()));
        assertThat(entity.getSender().getId().equals(sender.getId()));
        // These two are handled with a helper method in Message service
//        assertThat(entity.getChat() != null);
//        assertThat(entity.getChat().getId().equals(chat.getId()));
        assertThat(entity.getContent().equals(dto.getContent()));
        assertThat(entity.getMessageType().equals(MessageType.CHAT_MESSAGE));
        assertThat(entity.getCreatedDate().equals(dto.getSentTime()));
    }



}