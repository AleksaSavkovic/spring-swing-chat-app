package org.chatapp.server.service;

import lombok.RequiredArgsConstructor;
import org.chatapp.common.model.MessageStatusChangeDTO;
import org.chatapp.common.model.MessageStatusDTO;
import org.chatapp.common.model.StatusType;
import org.chatapp.server.entities.Message;
import org.chatapp.server.entities.MessageStatus;
import org.chatapp.server.entities.User;
import org.chatapp.server.mappers.MessageStatusMapper;
import org.chatapp.server.repositories.MessageStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageStatusServiceImpl implements MessageStatusService {

    private final MessageStatusRepository messageStatusRepository;
    private final MessageStatusMapper messageStatusMapper;

    @Transactional
    @Override
    public boolean updateMessageStatus(MessageStatusChangeDTO newStatus) {
        // Update status type for the message status
        int rowsAffected = messageStatusRepository.updateByMessageIdAndUserId(newStatus.getMessageId(), newStatus.getUserId(), newStatus.getStatus());

        // If not exactly one row was changed an error has occurred - either the status is already SENT after which it can
        // not change or the message status with the message and user ids provided could not be found
        if (rowsAffected != 1) {
            throw new IllegalStateException(
                    "MessageStatus not found or inconsistent for message " + newStatus.getMessageId()
            );
        }
        return true;
    }


    // Creates message status object when the message is sent
    @Transactional
    @Override
    public MessageStatusDTO createMessageStatus(UUID userId, UUID messageId) {
        MessageStatus statusToSave = MessageStatus.builder()
                .user(User.builder().id(userId).build())
                .message(Message.builder().id(messageId).build())
                .status(StatusType.SENT)
                .build();

        return messageStatusMapper.toDto(messageStatusRepository.save(statusToSave));
    }

    @Override
    public List<MessageStatusChangeDTO> getNewChatMsgStatuses(UUID chatId, Instant firstMsg, Instant lastSync) {
        return messageStatusRepository.getNewChatMsgStatuses(chatId, firstMsg, lastSync).stream()
                .map(messageStatusMapper::toChangeDto)
                .toList();
    }
}
