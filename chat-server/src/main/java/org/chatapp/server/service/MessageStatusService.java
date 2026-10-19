package org.chatapp.server.service;

import org.chatapp.common.model.MessageStatusChangeDTO;
import org.chatapp.common.model.MessageStatusDTO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageStatusService {

    // Update message status for user (input: userId, messageId, status)
    boolean updateMessageStatus(MessageStatusChangeDTO newStatus);

    // Creates message status object when the message is sent
    MessageStatusDTO createMessageStatus(UUID userId, UUID messageId);

    List<MessageStatusChangeDTO> getNewChatMsgStatuses(UUID chatId, Instant firstMsg, Instant lastSync);


}
