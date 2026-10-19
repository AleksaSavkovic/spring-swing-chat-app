package org.chatapp.server.socket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.common.model.*;
import org.chatapp.common.model.socket.transport.FirstMessageDTO;
import org.chatapp.common.model.socket.transport.SocketEventType;
import org.chatapp.common.model.socket.transport.UserStatus;
import org.chatapp.server.entities.User;
import org.chatapp.server.service.ChatService;
import org.chatapp.server.service.MessageService;
import org.chatapp.server.service.MessageStatusService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRouter {
    private final ConnectionManager connectionManager;
    private final ChatService chatService;
    private final MessageService messageService;
    private final MessageStatusService messageStatusService;

    public void routeMessage(MessageDTO message, ChatDTO chat) {
        // Checking if sender is a member of the chat, if not do not send the message
        if(!chatService.isChatMember(message.getChatId(), message.getSender().getId())) {
            return;
        }

        // Getting all recipients for the message
        List<User> recipients = chatService.getRecipients(message.getChatId(), message.getSender().getId());

        // Trying to move the save message logic to the service instead of doing it here
        // Creating a message status for the message for each user and marking it as sent
        List<MessageStatusDTO> statuses = new LinkedList<>();
        for(User recipient : recipients) {
            statuses.add(MessageStatusDTO.builder()
                            .userId(recipient.getId())
                            .status(StatusType.SENT)
                    .build());
        }
        message.setStatuses(statuses);


        // Checking if the message being replied to is in the same chat
        if(message.getReplyToId() != null) {
            MessageDTO reply = messageService.findMessageById(message.getReplyToId());
            // If message being replied to is not in the same chat do not send the message
            if(!message.getChatId().equals(reply.getChatId())){
                return;
            }
        }

        // Saving the message to the database
        MessageDTO savedMessage = messageService.saveMessage(message);

        // Notify the sender that the message has been sent
        ClientHandler senderSocket = connectionManager.getClients().get(message.getSender().getId());
        message.setId(savedMessage.getId());
        message.setStatuses(savedMessage.getStatuses());
        senderSocket.sendMessage(message, SocketEventType.SENT_MESSAGE);

        if (recipients.size() == 1) {
            if(chat!=null){
                sendFirstMessage(savedMessage, chat, recipients.get(0).getId());
            } else {
                sendRegularChatMessage(savedMessage, recipients);
            }
        } else {
            sendGroupChatMessage(savedMessage, recipients);
        }
    }

    // Method that creates and routes first message dto to the required user
    private void sendFirstMessage(MessageDTO message, ChatDTO chat, UUID recipientId) {
        // Get the client handler with the user id and send the message
        ClientHandler clientSocket = connectionManager.getClients().get(recipientId);
        if(clientSocket != null){
            // Create the message status dto and update the message status
            MessageStatusChangeDTO messageStatusDto = MessageStatusChangeDTO.builder()
                    .messageId(message.getId())
                    .userId(recipientId)
                    .status(StatusType.DELIVERED)
                    .build();
            messageStatusService.updateMessageStatus(messageStatusDto);

            // Create the FirstMessageDTO
            FirstMessageDTO firstMessageDTO = FirstMessageDTO.builder()
                    .messageDTO(message)
                    .chatDTO(chat)
                    .build();


            // User is online so we send the message and update the status to delivered
            clientSocket.sendFirstMessage(firstMessageDTO);

            // Send the status change to the sending user
            ClientHandler senderSocket = connectionManager.getClients().get(message.getSender().getId());
            // Change message type to DELIVERED_MESSAGE, and set the sender of this message the user who owns the status
            message.setContent(null);
            message.setSender(UserDTO.builder()
                    .id(recipientId)
                    .build());
            senderSocket.sendMessage(message, SocketEventType.DELIVERED_MESSAGE);
            // Also notify the user that the recipient is online because it is possible that the user wasn't in the presence
            // snapshot because the two users were not friends then.
            senderSocket.sendUserStatusChange(recipientId, UserStatus.CONNECTED);
        }
    }

    // Method that routes the CREATE_GROUP event to the required users
    public void routeCreateGroupEvent(ChatDTO groupChat){
        // Get all chat members from the chat that are not the creator
        List<UserDTO> recipients = groupChat.getUsers().stream()
                .filter(chatMemberDTO -> !chatMemberDTO.getRole().equals(ChatterRole.CREATOR))
                .map(chatMemberDTO -> UserDTO.builder()
                        .id(chatMemberDTO.getUserId())
                        .username(chatMemberDTO.getUsername())
                        .build())
                .toList();

        // Send the event to all online group members
        for (UserDTO user : recipients) {
            ClientHandler client = connectionManager.getClients().get(user.getId());
            if (client != null) {
                // Send the created group chat to the online user
                client.sendCreateGroup(groupChat);
            }
        }
    }

    // Method that routes the status change message when a user disconnects/connects to all the users
    public void handleUserStatusChange(UUID userId, UserStatus change){
        // Get all users that the user that disconnected shares chats with
        Set<UUID> connectedUsers = chatService.listUserChats(userId)
                .stream()
                .flatMap(chatDTO -> chatDTO.getUsers().stream()
                        .map(ChatMemberDTO::getUserId))
                .filter(uuid -> !uuid.equals(userId))
                .collect(Collectors.toSet());


        // Filter out all the non-online ones, because there is no need to notify them of changes via sockets
        Set<UUID> recipients = connectionManager.getOnlineFromUsers(connectedUsers);

        // Iterate through all authenticated clients and check if they are in the recipient set, if they are send them status change
        for (UUID recipientId : recipients) {
            ClientHandler client = connectionManager.getClients().get(recipientId);
            if (client != null) {
                client.sendUserStatusChange(userId, change);
            }
        }

    }

    // Find the client handler for the recipient and send the message to them
    private void sendRegularChatMessage(MessageDTO message, List<User> recipients) {
        UUID recipientId = recipients.get(0).getId();

        // Get the client handler with the user id and send the message
        ClientHandler clientSocket = connectionManager.getClients().get(recipientId);
        if(clientSocket != null){
            // Create the message status dto and update the message status
            MessageStatusChangeDTO messageStatusDto = MessageStatusChangeDTO.builder()
                            .messageId(message.getId())
                            .userId(recipientId)
                            .status(StatusType.DELIVERED)
                            .build();
            messageStatusService.updateMessageStatus(messageStatusDto);

            // User is online so we send the message and update the status to delivered
            clientSocket.sendMessage(message, SocketEventType.CHAT_MESSAGE);

            // Send the status change to the sending user
            ClientHandler senderSocket = connectionManager.getClients().get(message.getSender().getId());
            // Change message type to DELIVERED_MESSAGE, and set the sender of this message the user who owns the status
            message.setContent(null);
            message.setSender(UserDTO.builder()
                            .id(recipientId)
                            .build());
            senderSocket.sendMessage(message, SocketEventType.DELIVERED_MESSAGE);
        }
    }

    // Find the client handlers for each recipient and send the message to them via their socket
    private void sendGroupChatMessage(MessageDTO message, List<User> recipients) {
        // User for sending the status change to the sender user
        ClientHandler senderSocket = connectionManager.getClients().get(message.getSender().getId());

        // Create new message DTO with same values as original message other than different message type, so we
        // can use original message dto in following iterations of the loop when we send the normal message to recipient
        MessageDTO mutableMessage  = MessageDTO.builder()
                                .id(message.getId())
                                .sender(null)
                                .chatId(message.getChatId())
                                .messageType(message.getMessageType())
                                .build();

        // List of all users that actually received the message
        List<User> receivedMessage = new ArrayList<>();

        // Iterate through all authenticated clients and check if they are in the recipient set, if they are send the message
        for (User user : recipients) {
            ClientHandler client = connectionManager.getClients().get(user.getId());
            if (client != null) {
                // Send the actual message to the receiving user
                client.sendMessage(message, SocketEventType.CHAT_MESSAGE);

                // Add the user to the list of users that received the message
                receivedMessage.add(user);

                // Create the message status dto and update the message status
                MessageStatusChangeDTO messageStatusDto = MessageStatusChangeDTO.builder()
                        .messageId(message.getId())
                        .userId(user.getId())
                        .status(StatusType.DELIVERED)
                        .build();
                messageStatusService.updateMessageStatus(messageStatusDto);

                // Just change the user id of the mutable message so the sender knows for which user the message is delivered
                mutableMessage.setSender(UserDTO.builder()
                                        .id(user.getId())
                                        .build());
                // Send the message with delivered status to the original message sender
                senderSocket.sendMessage(mutableMessage, SocketEventType.DELIVERED_MESSAGE);
            }
        }

        // Send the delivered status to all other users as well
        for(User user : recipients) {
            ClientHandler client = connectionManager.getClients().get(user.getId());
            if (client != null) {
                for(User receivedMsgUser : receivedMessage) {
                    // Skip the user if the delivered status is about them
                    if(receivedMsgUser.equals(user)) {
                        continue;
                    }
                    // Just chang
                    // e the user id of the mutable message so status recipient knows for which user the message is delivered
                    mutableMessage.setSender(UserDTO.builder()
                            .id(receivedMsgUser.getId())
                            .build());

                    // Send the message with delivered status chat member
                    client.sendMessage(mutableMessage, SocketEventType.DELIVERED_MESSAGE);
                }
            }
        }
    }

    // This method handles bulk message delivered status updates
    public void handleBulkDeliveredChange(UUID userId, List<UUID> messageIds) {
        // Get messageDTO for each messageId, change to bulk function later on maybe
        List<MessageDTO> messageDTOS = new LinkedList<>();
        // Get message dto for each message id and save it to messageDTOs list
        for (UUID messageId : messageIds) {
            messageDTOS.add(messageService.findMessageById(messageId));
        }

        // Save all the message statuses to the database
        messageService.setBulkDeliveredStatus(userId, messageDTOS);

        // Get a map of all the users that need to receive updates for this bulk status update and a list of their messages
        // for which the status is updated
        Map<UUID, List<MessageDTO>> recipientsMessages = messageService.mapDeliveredStatusRecipients(userId, messageDTOS);

        // Iterate through user list
        for(UUID recipientId : recipientsMessages.keySet()){
            ClientHandler recipientSocket = connectionManager.getClients().get(recipientId);

            // If user is online send him a list of message ids which changed to delivered, and the deliveree user
            if (recipientSocket!=null) {
                List<UUID> messageIdList = recipientsMessages.get(recipientId).stream().map(MessageDTO::getId).toList();
                recipientSocket.sendBulkDeliveredStatus(userId, messageIdList);
            }
        }
    }

    // This method handles single message delivered status update
    public void handleDeliveredStatusChange(MessageDTO messageDTO) {
        // Getting all recipients(All users in the chat except the one who sends the update)
        List<User> recipients = chatService.getRecipients(messageDTO.getChatId(), messageDTO.getSender().getId());

        // Create status change dto and save status change to database
        MessageStatusChangeDTO statusChange = MessageStatusChangeDTO.builder()
                        .userId(messageDTO.getSender().getId())
                        .messageId(messageDTO.getId())
                        .status(StatusType.DELIVERED)
                        .build();
        messageStatusService.updateMessageStatus(statusChange);

        // Set message content to null so client can check if it's bulk update or not
        messageDTO.setContent(null);

        // Iterate through recipient list
        for(User recipient : recipients){
            ClientHandler recipientSocket = connectionManager.getClients().get(recipient.getId());

            // If recipient is online send him the status change
            if (recipientSocket!=null) {
                recipientSocket.sendMessage(messageDTO, SocketEventType.DELIVERED_MESSAGE);
            }
        }
    }

    // This method handles bulk message seen status updates
    public void handleBulkSeenChange(UUID userId, UUID chatId) {
        // Get all the users that need to receive the seen message status (all users in the chat except the status sender)
        List<User> recipients = chatService.getRecipients(chatId, userId);

        // Update the status to seen for all chat messages which are not "seen" for the user in chat
        messageService.setChatSeenStatuses(chatId, userId);

        // Iterate through recipient list
        for(User recipient : recipients){
            ClientHandler recipientSocket = connectionManager.getClients().get(recipient.getId());

            // If user is online send him the user id and the chat which user just opened
            if (recipientSocket!=null) {
                recipientSocket.sendBulkSeenStatus(userId, chatId);
            }
        }
    }

    // This method handles single message seen status update
    public void handleSeenStatusChange(MessageDTO messageDTO) {
        // Getting all recipients(All users in the chat except the one who sends the update)
        List<User> recipients = chatService.getRecipients(messageDTO.getChatId(), messageDTO.getSender().getId());

        // Create status change dto and save status change to database
        MessageStatusChangeDTO statusChange = MessageStatusChangeDTO.builder()
                .userId(messageDTO.getSender().getId())
                .messageId(messageDTO.getId())
                .status(StatusType.SEEN)
                .build();
        // Update the status to seen for the updated message status
        messageStatusService.updateMessageStatus(statusChange);

        // Set message content to null so client can check if it's bulk update or not
        messageDTO.setContent(null);

        // Iterate through recipient list
        for(User recipient : recipients){
            ClientHandler recipientSocket = connectionManager.getClients().get(recipient.getId());

            // If recipient is online send him the status change
            if (recipientSocket!=null) {
                recipientSocket.sendMessage(messageDTO, SocketEventType.SEEN_MESSAGE);
            }
        }

    }

    // Delete the message and notify all users that have received that message before the deletion
    public void handleMessageDelete(MessageDTO message) {
        // Getting all recipients for the message
        List<User> recipients = chatService.getRecipients(message.getChatId(), message.getSender().getId());

        // Delete the message from the database
        boolean deleted = messageService.deleteById(message.getId());

        // Get the sender socket
        ClientHandler senderSocket = connectionManager.getClients().get(message.getSender().getId());
        // If the message is not deleted notify the sender that deletion failed and don't proceed
        if(!deleted) {
            message.setActive(true);
            senderSocket.sendMessage(message, SocketEventType.DELETE_MESSAGE);
            return;
        }

        // If success notify the user and all recipients that the message has been deleted
        message.setActive(false);
            senderSocket.sendMessage(message, SocketEventType.DELETE_MESSAGE);
        // Iterate through all authenticated clients and check if they are in the recipient set, if they are send the message
        for (User user : recipients) {
            ClientHandler client = connectionManager.getClients().get(user.getId());
            if (client != null) {
                client.sendMessage(message, SocketEventType.DELETE_MESSAGE);
            }
        }
    }

    // Save method to database before sending it to individual users
    private void saveMessage(MessageDTO message){
        log.info("Saving message with id: {} to database", message.getId());
        messageService.saveMessage(message);
    }

}
