package org.chatapp.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.common.model.MessageDTO;
import org.chatapp.common.model.MessageStatusDTO;
import org.chatapp.common.model.StatusType;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.exceptions.ResourceForbiddenException;
import org.chatapp.server.entities.*;
import org.chatapp.server.mappers.MessageMapper;
import org.chatapp.server.mappers.MessageStatusMapper;
import org.chatapp.server.repositories.MessageRepository;
import org.chatapp.server.repositories.MessageStatusRepository;
import org.chatapp.server.validation.MessageValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;

    private final MessageMapper messageMapper;
    private final MessageStatusMapper messageStatusMapper;
    private final MessageValidator messageValidator;
    private final ChatService chatService;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 25;

    @Transactional
    @Override
    public Page<MessageDTO> getChatMessages(UUID chatId, Integer pageNumber, Integer pageSize, UUID activeUser) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize);

        // If user that requested the resource is not a member of the chat throw an exception
        if(!chatService.isChatMember(chatId, activeUser)) {
            throw new ResourceForbiddenException();
        }

        // Find the messages without statuses
        Page<Message> messages = messageRepository.findAllByChatId(chatId, pageRequest);

        // Get message status for retrieved messages
        // This is done in order to avoid  when sending status query for each message (N+1 query problem)
        List<UUID> messageIds = messages.getContent().stream().map(Message::getId).toList();
        List<MessageStatus> statuses = messageStatusRepository.findByMessageIdIn(messageIds);

        // Group the statuses with the appropriate messages
        Map<UUID, List<MessageStatus>> statusMap =
                statuses.stream().collect(Collectors.groupingBy(ms -> ms.getMessage().getId()));

        // Map all the messages to their dtos, for each message convert the message statuses to dtos and add them to the message dto
        return messages.map(message -> {
            MessageDTO dto = messageMapper.toDto(message);

            List<MessageStatusDTO> statusDTOs = statusMap.getOrDefault(message.getId(), List.of())
                    .stream()
                    .map(messageStatusMapper::toDto)
                    .toList();

            dto.setStatuses(statusDTOs);
            return dto;
        });
    }

    @Override
    public MessageDTO saveMessage(MessageDTO messageDTO) {
        // Validates message and throws exception if required data is not there
        messageValidator.validateMessageCreate(messageDTO);

        return toMessageDto(messageRepository.save(toMessageEntity(messageDTO)));
    }

    @Override
    public MessageDTO toMessageDto(Message message) {
        MessageDTO dto = messageMapper.toDto(message);

        List<MessageStatusDTO> statuses = new ArrayList<>();
        message.getMessageStatuses().forEach(messageStatus -> {
            MessageStatusDTO statusDto = messageStatusMapper.toDto(messageStatus);
            statuses.add(statusDto);
        });

        dto.setStatuses(statuses);
        return dto;
    }

    public Message toMessageEntity(MessageDTO dto) {
        Message entity = messageMapper.toEntity(dto);

        // Check if there is a replyToId in the dto, if there is get the corresponding message and set it as reply to
        // parameter for the message entity, if the message is not found throw an exception because the user should not
        // put uuids which are non-existent as the replyToId value.
        if(dto.getReplyToId() != null) {
            entity.setReplyTo(messageRepository.findById(dto.getReplyToId()).orElseThrow(IllegalArgumentException::new));
        }

        // Find the chat by id
        Optional<Chat> chatEntity = chatService.findChatEntityById(dto.getChatId());

        // If a chat with the requested id is found set the chat to the message entity
        chatEntity.ifPresent(entity::setChat);

        // If the message has status set them here
        Set<MessageStatus> statuses = new HashSet<>();
        if(!dto.getStatuses().isEmpty()){
            dto.getStatuses().forEach(messageStatusDTO -> {
                MessageStatus statusEntity = messageStatusMapper.toEntity(messageStatusDTO);
                statusEntity.setMessage(entity);
                statuses.add(statusEntity);
            });
        }

        entity.setMessageStatuses(statuses);
        return entity;
    }

    // Updates all message statuses in the chat for given user to SEEN
    @Transactional
    @Override
    public void setChatSeenStatuses(UUID chatId, UUID userId) {
        messageStatusRepository.updateChatMessagesStatus(chatId, userId, StatusType.SEEN);
    }

    @Transactional
    @Override
    public void setBulkDeliveredStatus(UUID userID, List<MessageDTO> messages) {
        // Get a list of message ids from the message dto list
        List<UUID> messageIds = messages.stream().map(MessageDTO::getId).toList();

        // Run bulk status update in repository
        messageStatusRepository.updateByMessageListAndUserId(userID, messageIds, StatusType.DELIVERED);
    }

    @Transactional
    @Override
    public MessageDTO findMessageById(UUID messageId) {
        // Find message by id and return it as DTO, if no message found throw not found exception
        Message foundMessage = messageRepository.findById(messageId).orElseThrow(NotFoundException::new);
        return messageMapper.toDto(foundMessage);
    }

    // Find all the messages with given ids and return them as message dto's
    @Transactional
    @Override
    public List<MessageDTO> findMessagesByIdIn(Set<UUID> messageUUIDs) {
        List<Message> foundMessages = messageRepository.findByIdIn(messageUUIDs);
        return foundMessages.stream().map(messageMapper::toDto).toList();
    }

    @Override
    public List<Message> getNewMessages(UUID userId, Instant lastSync){
        return messageRepository.syncUserMessagesSince(userId, lastSync);
    }


    // This method maps all the users that should receive message status updates with the status updates for appropriate message
    @Override
    public Map<UUID, List<MessageDTO>> mapDeliveredStatusRecipients(UUID userId, List<MessageDTO> messageDTOs) {
        // Gets chat objects along with the chat members loaded for each chat whose id is in the messageDTO list
        Map<UUID,Chat> messageChats = chatService.getChatsWithUsers(messageDTOs.stream().map(MessageDTO::getChatId).toList());


        // Map of users and the messages they should receive the delivered update for
        Map<UUID, List<MessageDTO>> recipientMessages = new HashMap<>();
        for(MessageDTO messageDto : messageDTOs) {

            // Iterate through all members of the chat in which the status message is sent
            for(ChatMember recipient : messageChats.get(messageDto.getChatId()).getUsers()){
                // Skip the sender of the status message
                if(recipient.getUser().getId().equals(userId)){
                    continue;
                }
                recipientMessages.computeIfAbsent(recipient.getUser().getId(), k -> new LinkedList<>())
                        .add(messageDto);
            }
        }

        // Return the map of recipients and their messages
        return recipientMessages;
    }

    // Soft delete message with given id if it exists, otherwise just return false
    @Transactional
    @Override
    public boolean deleteById(UUID messageId) {
        if(messageRepository.existsById(messageId)) {
            messageRepository.setInactiveById(messageId, Instant.now());
            return true;
        }
        return false;
    }

    // Builds PageRequest with the page number and size of the page which the user selected (or default if none)
    public PageRequest buildPageRequest (Integer pageNumber, Integer pageSize){
        int queryPageNumber;
        int queryPageSize;

        if(pageNumber != null && pageNumber > 0){
            queryPageNumber = pageNumber - 1;
        } else {
            queryPageNumber = DEFAULT_PAGE;
        }

        if (pageSize == null){
            queryPageSize = DEFAULT_PAGE_SIZE;
        } else {
            if(pageSize > 1000){
                queryPageSize = 1000;
            } else {
                queryPageSize = pageSize;
            }
        }
        Sort sort = Sort.by(Sort.Order.desc("createdDate"));

        return PageRequest.of(queryPageNumber, queryPageSize, sort);
    }
}
