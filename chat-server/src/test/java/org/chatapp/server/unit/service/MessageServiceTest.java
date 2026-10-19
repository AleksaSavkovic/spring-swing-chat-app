package org.chatapp.server.unit.service;

import org.chatapp.common.model.MessageDTO;
import org.chatapp.common.model.MessageStatusDTO;
import org.chatapp.server.exceptions.ResourceForbiddenException;
import org.chatapp.server.entities.*;
import org.chatapp.server.mappers.MessageMapper;
import org.chatapp.server.mappers.MessageStatusMapper;
import org.chatapp.server.repositories.MessageRepository;
import org.chatapp.server.repositories.MessageStatusRepository;
import org.chatapp.server.service.ChatService;
import org.chatapp.server.service.MessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;


@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

    @Mock
    MessageRepository messageRepository;
    @Mock
    MessageStatusRepository messageStatusRepository;
    @Mock
    MessageMapper messageMapper;
    @Mock
    MessageStatusMapper messageStatusMapper;
    @Mock
    ChatService chatService;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 25;

    @InjectMocks
    MessageServiceImpl messageService;

    @Test
    void getChatMessages_ShouldReturnMessages() {
        // Given
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .build();

        MessageStatus status = MessageStatus.builder()
                .message(message)
                .build();

        MessageDTO messageDto = new MessageDTO();
        MessageStatusDTO statusDto = new MessageStatusDTO();

        Page<Message> messagePage = new PageImpl<>(List.of(message));

        given(chatService.isChatMember(chatId, userId)).willReturn(true);
        given(messageRepository.findAllByChatId(eq(chatId), any(PageRequest.class)))
                .willReturn(messagePage);
        given(messageStatusRepository.findByMessageIdIn(List.of(message.getId())))
                .willReturn(List.of(status));

        given(messageMapper.toDto(message)).willReturn(messageDto);
        given(messageStatusMapper.toDto(status)).willReturn(statusDto);

        // When
        Page<MessageDTO> result =
                messageService.getChatMessages(chatId, 0, 20, userId);

        // Then
        assertThat(result.getContent()).hasSize(1);

        MessageDTO returned = result.getContent().get(0);

        assertThat(returned).isSameAs(messageDto);
        assertThat(returned.getStatuses())
                .containsExactly(statusDto);

        verify(chatService).isChatMember(chatId, userId);
        verify(messageRepository).findAllByChatId(eq(chatId), any(PageRequest.class));
        verify(messageStatusRepository).findByMessageIdIn(List.of(message.getId()));
        verify(messageMapper).toDto(message);
        verify(messageStatusMapper).toDto(status);
    }

    @Test
    void getChatMessages_ShouldThrow() {
        // Given
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(chatService.isChatMember(chatId, userId)).willReturn(false);

        // Then
        assertThrows(ResourceForbiddenException.class, () -> {
            messageService.getChatMessages(chatId, 0, 20, userId);
        });

        verify(chatService).isChatMember(chatId, userId);

        verifyNoInteractions(
                messageRepository,
                messageStatusRepository,
                messageMapper,
                messageStatusMapper
        );
    }

    @Test
    void mapDeliveredStatusRecipients_ShouldMapMessagesToAllRecipientsExceptSender() {
        // Given
        UUID senderId = UUID.randomUUID();
        UUID recipient1Id = UUID.randomUUID();
        UUID recipient2Id = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        MessageDTO message1 = MessageDTO.builder()
                .id(UUID.randomUUID())
                .chatId(chatId)
                .build();

        MessageDTO message2 = MessageDTO.builder()
                .id(UUID.randomUUID())
                .chatId(chatId)
                .build();

        List<MessageDTO> messages = List.of(message1, message2);

        Chat chat = Chat.builder()
                .id(chatId)
                .users(Set.of(
                        ChatMember.builder()
                                .user(User.builder().id(senderId).build())
                                .build(),
                        ChatMember.builder()
                                .user(User.builder().id(recipient1Id).build())
                                .build(),
                        ChatMember.builder()
                                .user(User.builder().id(recipient2Id).build())
                                .build()
                ))
                .build();

        given(chatService.getChatsWithUsers(anyList()))
                .willReturn(Map.of(chatId, chat));

        // When
        Map<UUID, List<MessageDTO>> result =
                messageService.mapDeliveredStatusRecipients(senderId, messages);

        // Then
        assertThat(result).hasSize(2);

        assertThat(result.get(recipient1Id))
                .containsExactly(message1, message2);

        assertThat(result.get(recipient2Id))
                .containsExactly(message1, message2);

        assertThat(result).doesNotContainKey(senderId);

        verify(chatService).getChatsWithUsers(anyList());
    }

    @Test
    void mapDeliveredStatusRecipients_ShouldMapMessagesToRecipientsInTheirChat() {
        // Given
        UUID senderId = UUID.randomUUID();

        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();

        UUID chat1Id = UUID.randomUUID();
        UUID chat2Id = UUID.randomUUID();

        MessageDTO message1 = MessageDTO.builder()
                .id(UUID.randomUUID())
                .chatId(chat1Id)
                .build();

        MessageDTO message2 = MessageDTO.builder()
                .id(UUID.randomUUID())
                .chatId(chat2Id)
                .build();

        Chat chat1 = Chat.builder()
                .id(chat1Id)
                .users(Set.of(
                        ChatMember.builder()
                                .user(User.builder().id(senderId).build())
                                .build(),
                        ChatMember.builder()
                                .user(User.builder().id(user1).build())
                                .build(),
                        ChatMember.builder()
                                .user(User.builder().id(user2).build())
                                .build()
                ))
                .build();

        Chat chat2 = Chat.builder()
                .id(chat2Id)
                .users(Set.of(
                        ChatMember.builder()
                                .user(User.builder().id(senderId).build())
                                .build(),
                        ChatMember.builder()
                                .user(User.builder().id(user3).build())
                                .build()
                ))
                .build();

        given(chatService.getChatsWithUsers(anyList()))
                .willReturn(Map.of(
                        chat1Id, chat1,
                        chat2Id, chat2
                ));

        // When
        Map<UUID, List<MessageDTO>> result =
                messageService.mapDeliveredStatusRecipients(
                        senderId,
                        List.of(message1, message2)
                );

        // Then
        assertThat(result).hasSize(3);

        assertThat(result.get(user1))
                .containsExactly(message1);

        assertThat(result.get(user2))
                .containsExactly(message1);

        assertThat(result.get(user3))
                .containsExactly(message2);

        assertThat(result).doesNotContainKey(senderId);

        verify(chatService).getChatsWithUsers(anyList());
    }

    @ParameterizedTest
    @MethodSource("pages")
    void buildPageRequest_ShouldIgnoreInputWhenInvalid(Integer pageNumber, Integer pageSize, Integer expectedPageNumber, Integer expectedPageSize) {
        // When
        PageRequest pageRequest = messageService.buildPageRequest(pageNumber, pageSize);

        // Then
        assertThat(pageRequest.getPageNumber()).isEqualTo(expectedPageNumber);
        assertThat(pageRequest.getPageSize()).isEqualTo(expectedPageSize);
    }

    static Stream<Arguments> pages() {
        return Stream.of(
                Arguments.of(null, null, DEFAULT_PAGE, DEFAULT_PAGE_SIZE),
                Arguments.of(null, 20, DEFAULT_PAGE, 20),
                Arguments.of(-3, 20, DEFAULT_PAGE, 20),
                Arguments.of(3, 20, 2, 20),
                Arguments.of(3, 5000, 2, 1000)
        );
    }


}
