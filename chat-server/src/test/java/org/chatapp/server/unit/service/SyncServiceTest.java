package org.chatapp.server.unit.service;

import org.chatapp.common.model.*;
import org.chatapp.server.entities.Chat;
import org.chatapp.server.entities.ChatMember;
import org.chatapp.server.entities.Message;
import org.chatapp.server.entities.MessageStatus;
import org.chatapp.server.service.ChatService;
import org.chatapp.server.service.MessageService;
import org.chatapp.server.service.MessageStatusService;
import org.chatapp.server.service.SyncServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private ChatService chatService;

    @Mock
    private MessageService messageService;

    @Mock
    private MessageStatusService messageStatusService;

    @InjectMocks
    private SyncServiceImpl syncService;

    @Test
    void syncUserDataLastSyncIsNull() {
        // Given
        UUID userId = UUID.randomUUID();

        UUID chatId = UUID.randomUUID();

        ChatDTO chat = ChatDTO.builder()
                .id(chatId)
                .build();

        MessageDTO message = MessageDTO.builder().build();

        given(chatService.listUserChats(userId))
                .willReturn(List.of(chat));

        given(messageService.getChatMessages(chatId, 1, 25, userId))
                .willReturn(new PageImpl<>(List.of(message)));

        // When
        SyncResponseDTO response = syncService.syncUserData(userId, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNewChats()).containsExactly(chat);
        assertThat(response.getNewMessages()).containsExactly(message);
        assertThat(response.getServerTime()).isNotNull();

        verify(chatService).listUserChats(userId);
        verify(messageService).getChatMessages(chatId, 1, 25, userId);

        verify(chatService, never()).getNewChats(any(), any());
        verify(messageService, never()).getNewMessages(any(), any());
    }

    @Test
    void syncUserDataLastSyncExists() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant lastSync = Instant.parse("2025-01-01T00:00:00Z");

        Chat chat = mock(Chat.class);
        ChatMember newMember = mock(ChatMember.class);
        ChatMember oldMember = mock(ChatMember.class);

        given(newMember.getCreatedDate()).willReturn(lastSync.plusSeconds(10));

        given(oldMember.getCreatedDate()).willReturn(lastSync.minusSeconds(10));
        given(chat.getUsers()).willReturn(Set.of(newMember, oldMember));

        ChatDTO chatDto = ChatDTO.builder().build();

        Message message = mock(Message.class);

        MessageStatus newStatus = mock(MessageStatus.class);
        MessageStatus oldStatus = mock(MessageStatus.class);

        given(newStatus.getCreatedDate()).willReturn(lastSync.plusSeconds(5));

        given(oldStatus.getCreatedDate()).willReturn(lastSync.minusSeconds(5));
        given(message.getMessageStatuses()).willReturn(Set.of(newStatus, oldStatus));

        MessageDTO messageDto = MessageDTO.builder().build();

        given(chatService.getNewChats(userId, lastSync))
                .willReturn(List.of(chat));

        given(chatService.toChatDto(chat, userId))
                .willReturn(chatDto);

        given(messageService.getNewMessages(userId, lastSync))
                .willReturn(List.of(message));

        given(messageService.toMessageDto(message))
                .willReturn(messageDto);

        // When
        SyncResponseDTO response = syncService.syncUserData(userId, lastSync);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNewChats()).containsExactly(chatDto);
        assertThat(response.getNewMessages()).containsExactly(messageDto);

        verify(chatService).getNewChats(userId, lastSync);
        verify(messageService).getNewMessages(userId, lastSync);
    }

    @Test
    void syncUserDataLastSyncAfterCurrentTime(){
        UUID userId = UUID.randomUUID();
        // Using fixed value for Instant.now for test consistency
        Instant fixedNow = Instant.parse("2026-06-23T12:00:00Z");
        // Last sync is after current time
        Instant lastSync = fixedNow.plus(5, ChronoUnit.MINUTES);

        try(MockedStatic<Instant> mockedInstant = Mockito.mockStatic(Instant.class)) {
            given(Instant.now()).willReturn(fixedNow);

            // This class should throw an exception
            assertThrows(IllegalArgumentException.class, () -> {
                syncService.syncUserData(userId, lastSync);
            });
        }
    }

    @Test
    void syncChatMsgStatusesDelegateToService() {
        // Given
        UUID chatId = UUID.randomUUID();

        Instant firstMessage = Instant.parse("2025-01-01T00:00:00Z");
        Instant lastSync = Instant.parse("2025-02-01T00:00:00Z");

        MessageStatusChangeDTO dto = MessageStatusChangeDTO.builder().build();

        given(messageStatusService.getNewChatMsgStatuses(chatId, firstMessage, lastSync))
                .willReturn(List.of(dto));

        // When
        List<MessageStatusChangeDTO> result =
                syncService.syncChatMsgStatuses(chatId, firstMessage, lastSync);

        // Then
        assertThat(result).containsExactly(dto);

        verify(messageStatusService)
                .getNewChatMsgStatuses(chatId, firstMessage, lastSync);
    }

    @Test
    void syncMessageStatusesLastSyncAfterCurrentTime(){
        // Given
        UUID chatId = UUID.randomUUID();
        // Using fixed value for Instant.now for test consistency
        Instant fixedNow = Instant.parse("2026-06-23T12:00:00Z");
        // First message timestamp irrelevant
        Instant firstMessage = mock(Instant.class);
        // Last sync is after current time
        Instant lastSync = fixedNow.plus(5, ChronoUnit.MINUTES);

        // Then
        try(MockedStatic<Instant> mockedInstant = Mockito.mockStatic(Instant.class)) {
            given(Instant.now()).willReturn(fixedNow);

            // This class should throw an exception
            assertThrows(IllegalArgumentException.class, () -> {
                syncService.syncChatMsgStatuses(chatId,firstMessage, lastSync);
            });
        }
    }

    @Test
    void syncMessageStatusesFirstMessageAfterCurrentTime(){
        // Given
        UUID chatId = UUID.randomUUID();
        // Using fixed value for Instant.now for test consistency
        Instant fixedNow = Instant.parse("2026-06-23T12:00:00Z");
        // First message timestamp irrelevant
        Instant firstMessage = fixedNow.plus(5, ChronoUnit.MINUTES);

        try(MockedStatic<Instant> mockedInstant = Mockito.mockStatic(Instant.class)) {
            given(Instant.now()).willReturn(fixedNow);

            // This class should throw an exception
            assertThrows(IllegalArgumentException.class, () -> {
                syncService.syncChatMsgStatuses(chatId,firstMessage, null);
            });
        }
    }

    @Test
    void syncMessageStatusesLastSyncBeforeFirstMessage(){
        // Given
        UUID chatId = UUID.randomUUID();
        // First message is after last sync
        Instant firstMsg = Instant.parse("2026-06-23T12:00:00Z");
        // Last sync is before first message
        Instant lastSync = Instant.parse("2026-06-23T11:59:59Z");

        // Then
        // This class should throw an exception
        assertThrows(IllegalArgumentException.class, () -> {
            syncService.syncChatMsgStatuses(chatId, firstMsg, lastSync);
        });
    }

    @Test
    void syncUserDataWhenNothingChanged() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant lastSync = Instant.now();

        given(chatService.getNewChats(userId, lastSync))
                .willReturn(List.of());

        given(messageService.getNewMessages(userId, lastSync))
                .willReturn(List.of());

        // When
        SyncResponseDTO response = syncService.syncUserData(userId, lastSync);

        // Then
        assertThat(response.getNewChats()).isEmpty();
        assertThat(response.getNewMessages()).isEmpty();
        assertThat(response.getServerTime()).isNotNull();

        verify(chatService, never()).toChatDto(any(), any());
        verify(messageService, never()).toMessageDto(any());
    }

}