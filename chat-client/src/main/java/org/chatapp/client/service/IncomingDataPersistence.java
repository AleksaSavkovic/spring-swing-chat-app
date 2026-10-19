package org.chatapp.client.service;

import org.chatapp.client.repository.*;
import org.chatapp.common.model.*;

import java.util.List;
import java.util.UUID;

public class IncomingDataPersistence {

    private final DatabaseManager databaseManager;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;


    public IncomingDataPersistence(DatabaseManager databaseManager,
                                   ChatRepository chatRepository,
                                   MessageRepository messageRepository){
        this.databaseManager = databaseManager;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
    }


    // Update all the message statuses for a user in chat to seen
    public void bulkSeenUpdate(UUID userId, UUID chatId) {
        databaseManager.executeTransaction(() -> messageRepository.bulkSeenUpdate(userId, chatId));
    }

    // Update all the message statuses for a user in provided messages to delivered
    public void bulkDeliveredUpdate(UUID userId, List<UUID> messageIds) {
        databaseManager.executeTransaction(() -> messageRepository.bulkDeliveredUpdate(userId, messageIds));
    }

    // Saves the message and it's statuses to the database and returns the newly created object
    public MessageDTO insertNewMessage(MessageDTO messageDTO){
        databaseManager.executeTransaction(() -> {
            // Insert message
            messageRepository.insertMessage(messageDTO, false);
            // Insert message statuses for the message
            messageRepository.insertMessageStatuses(messageDTO);
        });

        // Returning the newly inserted message from the local database
        return databaseManager.executeQuery(() -> messageRepository.findMessageById(messageDTO.getId()));
    }

    // Update status for message status object
    public void singleStatusUpdate(UUID userId, UUID messageId, StatusType status){
        MessageStatusChangeDTO statusChangeDTO = MessageStatusChangeDTO.builder()
                .userId(userId)
                .messageId(messageId)
                .status(status)
                .build();
        databaseManager.executeTransaction(() -> messageRepository.updateMessageStatus(statusChangeDTO));
    }

    // Checks if the chat exists in the local database, and if it does not add it to the database
    public void saveChatIfNotInLocal(ChatDTO chat) {
        if(chatRepository.findChatById(chat.getId()) == null) {
            databaseManager.executeTransaction(() -> chatRepository.insertChat(chat));
        }
    }

    // Returns true if message with given UUID is in the local database otherwise returns false
    public boolean findMessageByIdInLocal(UUID messageId) {
        MessageDTO foundMessage = databaseManager.executeQuery(() -> messageRepository.findMessageById(messageId));
        return foundMessage != null;
    }

    // Delete (set to null) the content of the message with given id
    public void deleteMessage(UUID messageId) {
        databaseManager.executeTransaction(() -> messageRepository.deleteMessageContent(messageId));
    }
}
