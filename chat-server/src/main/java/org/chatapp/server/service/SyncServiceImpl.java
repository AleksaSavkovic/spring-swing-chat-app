package org.chatapp.server.service;

import lombok.RequiredArgsConstructor;
import org.chatapp.common.model.*;
import org.chatapp.server.entities.Chat;
import org.chatapp.server.entities.ChatMember;
import org.chatapp.server.entities.Message;
import org.chatapp.server.entities.MessageStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class SyncServiceImpl implements SyncService {
    private final ChatService chatService;
    private final MessageService messageService;
    private final MessageStatusService messageStatusService;

    @Override
    public SyncResponseDTO syncUserData(UUID userId, Instant lastSync) {
        // In the case there is no timestamp just get all user chats, a page's worth of messages for each chat alongside their statuses
        if (lastSync == null) {
            return getDataWithoutSync(userId);
        }

        // If last sync is after current time throw an illegal argument exception
        if (lastSync.isAfter(Instant.now())) {
            throw new IllegalArgumentException();
        }

        // Get all updated/newly created chats
        List<Chat> syncedChats = chatService.getNewChats(userId, lastSync);

        // Remove all information already in the client side database from the chat list
        List<ChatDTO> chatsFiltered = new ArrayList<>();
        for (Chat syncedChat : syncedChats) {
            // Get all the new chat members of the chat which were added or updated after last sync
            Set<UUID> chatMembersToKeep = syncedChat.getUsers().stream().filter(chatMember ->
                    (chatMember.getCreatedDate().isAfter(lastSync) ||
                            (chatMember.getUpdateDate() != null && chatMember.getUpdateDate().isAfter(lastSync))))
                    .map(ChatMember::getId)
                    .collect(Collectors.toSet());

            ChatDTO filteredDto = chatService.toChatDto(syncedChat, userId);

            filteredDto.setUsers(filteredDto.getUsers().stream()
                    .filter(userDto -> chatMembersToKeep.contains(userDto.getId()))
                    .toList());


            chatsFiltered.add(filteredDto);
        }

        // Get all updated/newly created messages
        List<Message> syncedMessages = messageService.getNewMessages(userId, lastSync);

        // Remove all information already in the client database from the message list
        List<MessageDTO> messagesFiltered = new ArrayList<>();
        Set<UUID> referenceOnlyUUIDs = new HashSet<>();
        for (Message syncedMessage : syncedMessages) {
            // Convert message entity to DTO
            MessageDTO filteredDto = messageService.toMessageDto(syncedMessage);
            // Get all the message statuses for the message that were updated or created after last sync
            Set<UUID> statusesToKeep = syncedMessage.getMessageStatuses().stream()
                    .filter(messageStatus ->
                    (messageStatus.getCreatedDate().isAfter(lastSync) ||
                            (messageStatus.getUpdateDate() != null && messageStatus.getUpdateDate().isAfter(lastSync))))
                    .map(MessageStatus::getId)
                    .collect(Collectors.toSet());
            // Update the statuses in the filtered DTO's status list
            filteredDto.setStatuses(filteredDto.getStatuses().stream()
                    .filter(statusDTO -> statusesToKeep.contains(statusDTO.getId()))
                    .toList());
            // Add the filtered dto to the messagesFiltered list
            messagesFiltered.add(filteredDto);

            // Add the replyToId to referenceOnlyUUIDs if it is not null
            if (filteredDto.getReplyToId() != null) {
                referenceOnlyUUIDs.add(filteredDto.getReplyToId());
            }
        }

        // Set the content to null for all messages that are inactive
        messagesFiltered.stream()
                .filter(messageDTO -> !messageDTO.isActive())
                .forEach(messageDTO -> messageDTO.setContent(null));

        // Get all the messages which are not in this list but are replied to by messages in this list
        referenceOnlyUUIDs = referenceOnlyUUIDs.stream()
                .filter(uuid -> messagesFiltered.stream().noneMatch(messageDTO -> messageDTO.getId().equals(uuid)))
                .collect(Collectors.toSet());

        // Get the reference only DTOs, remove the reply id for them as they are only to be shown in messages replying to
        // them and not by themselves where the messages being replied to is visible
        List<MessageDTO> referenceOnlyMessages = messageService.findMessagesByIdIn(referenceOnlyUUIDs);
        referenceOnlyMessages.forEach(messageDTO -> messageDTO.setReplyToId(null));

        return SyncResponseDTO.builder()
                .newChats(chatsFiltered)
                .newMessages(messagesFiltered)
                .referenceOnlyMessages(referenceOnlyMessages)
                .serverTime(Instant.now())
                .build();
    }

    // TODO Consider changing this in the future to show all unread messages if there is more unread than one page or
    //  some other practical way to load the data for the first load. Also the filtering for deleted messages is also
    //  too much and really should be written cleaner.
    // This method combines a list of all user chats together with the chat members and users and a page of messages for each chat
    private SyncResponseDTO getDataWithoutSync(UUID userId) {
        // Get all user chats
        List<ChatDTO> chats = chatService.listUserChats(userId);

        // Get a page's worth of messages for each chat
        List<MessageDTO> messages = new ArrayList<>();
        chats.forEach(chatDTO -> messages
                .addAll(messageService.getChatMessages(chatDTO.getId(), 1, 25, userId).getContent()));

        // Set the content to null for all messages that are inactive
        messages.stream()
                .filter(messageDTO -> !messageDTO.isActive())
                .forEach(messageDTO -> messageDTO.setContent(null));

        // Get all replyToIds
        Set<UUID> referenceOnlyUUIDs = messages.stream()
                .map(MessageDTO::getReplyToId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Remove all reply to ids which are already in the messages list
        referenceOnlyUUIDs = referenceOnlyUUIDs.stream()
                .filter(uuid -> messages.stream().noneMatch(messageDTO -> messageDTO.getId().equals(uuid)))
                .collect(Collectors.toSet());

        // Get the reference only DTOs, remove the reply id for them as they are only to be shown in messages replying to
        // them and not by themselves where the messages being replied to is visible
        List<MessageDTO> referenceOnlyMessages = messageService.findMessagesByIdIn(referenceOnlyUUIDs);
        referenceOnlyMessages.forEach(messageDTO -> messageDTO.setReplyToId(null));


        return SyncResponseDTO.builder()
                .newChats(chats)
                .newMessages(messages)
                .referenceOnlyMessages(referenceOnlyMessages)
                .serverTime(Instant.now())
                .build();
    }

    @Override
    public List<MessageStatusChangeDTO> syncChatMsgStatuses(UUID chatId, Instant firstMsg, Instant lastSync) {
        if (lastSync != null && (lastSync.isBefore(firstMsg) || lastSync.isAfter(Instant.now()))
                || firstMsg.isAfter(Instant.now())) {
            throw new IllegalArgumentException();
        }
        return messageStatusService.getNewChatMsgStatuses(chatId, firstMsg, lastSync);
    }

}
