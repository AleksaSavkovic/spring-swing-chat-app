package org.chatapp.server.unit.service;

import org.chatapp.common.model.MessageStatusChangeDTO;
import org.chatapp.common.model.StatusType;
import org.chatapp.server.mappers.MessageStatusMapper;
import org.chatapp.server.repositories.MessageStatusRepository;
import org.chatapp.server.service.MessageStatusServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MessageStatusServiceTest {

    @Mock
    MessageStatusRepository messageStatusRepository;
    @Mock
    MessageStatusMapper messageStatusMapper;

    @InjectMocks
    MessageStatusServiceImpl messageStatusService;

    @Test
    void updateMessageStatus_ShouldReturnTrue(){
        // Given
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        MessageStatusChangeDTO newStatus = MessageStatusChangeDTO.builder()
                .messageId(messageId)
                .userId(userId)
                .status(StatusType.DELIVERED)
                .build();

        given(messageStatusRepository.updateByMessageIdAndUserId(messageId, userId, StatusType.DELIVERED)).willReturn(1);

        // When
        boolean result = messageStatusService.updateMessageStatus(newStatus);

        // Then
        assertThat(result).isTrue();
        verify(messageStatusRepository).updateByMessageIdAndUserId(messageId, userId, StatusType.DELIVERED);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 5, -1})
    void updateMessageStatus_ShouldThrow_WhenRowsAffectedIsNotOne(int rowsAffected) {
        // Given
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        MessageStatusChangeDTO newStatus = MessageStatusChangeDTO.builder()
                .messageId(messageId)
                .userId(userId)
                .status(StatusType.DELIVERED)
                .build();

        given(messageStatusRepository.updateByMessageIdAndUserId(
                messageId,
                userId,
                StatusType.DELIVERED))
                .willReturn(rowsAffected);

        // When / Then
        assertThrows(IllegalStateException.class, () -> {
            messageStatusService.updateMessageStatus(newStatus);
        });

        verify(messageStatusRepository)
                .updateByMessageIdAndUserId(
                        messageId,
                        userId,
                        StatusType.DELIVERED);
    }
}
