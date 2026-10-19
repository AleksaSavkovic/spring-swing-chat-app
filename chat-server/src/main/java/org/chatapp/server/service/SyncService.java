package org.chatapp.server.service;

import org.chatapp.common.model.MessageStatusChangeDTO;
import org.chatapp.common.model.SyncResponseDTO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SyncService {

    SyncResponseDTO syncUserData(UUID userId, Instant lastSync);

    List<MessageStatusChangeDTO> syncChatMsgStatuses(UUID chatId, Instant firstMsg, Instant lastSync);
}
