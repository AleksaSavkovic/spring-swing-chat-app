package org.chatapp.server.service;

import org.chatapp.common.model.ChatDTO;
import org.chatapp.common.model.MessageDTO;
import org.chatapp.server.entities.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface MessageService {
    Page<MessageDTO> getChatMessages(UUID chatId, Integer pageNumber, Integer pageSize, UUID activeUser);

    MessageDTO saveMessage(MessageDTO messageDTO);

    Message toMessageEntity(MessageDTO dto);

    MessageDTO toMessageDto(Message message);

    void setChatSeenStatuses(UUID chatId, UUID userId);

    void setBulkDeliveredStatus(UUID userID, List<MessageDTO> messages);

    MessageDTO findMessageById(UUID messageId);

    List<MessageDTO> findMessagesByIdIn(Set<UUID> messageUUIDs);

    List<Message> getNewMessages(UUID userId, Instant lastSync);

    Map<UUID, List<MessageDTO>> mapDeliveredStatusRecipients(UUID userId, List<MessageDTO> messageDTOs);

    boolean deleteById(UUID messageId);
}
