package org.chatapp.client.repository;

import org.chatapp.client.ui.records.MessageStatusDisplay;
import org.chatapp.common.model.*;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class MessageRepository {

    private final Connection connection;

    public MessageRepository(DatabaseManager databaseManager) {
        this.connection = databaseManager.getConnection();
    }

    // Inserts message dto into the local database
    public boolean insertMessage(MessageDTO message, boolean referenceOnly){
        String insertString = "INSERT INTO message(id, chat_id, sender_id, content, message_type, sent_time, reply_to_id, reference_only) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try(PreparedStatement stmt = connection.prepareStatement(insertString))
        {
            // Place all needed values into prepared statement
            stmt.setString(1, message.getId().toString());
            stmt.setString(2, message.getChatId().toString());
            stmt.setString(3, message.getSender().getId().toString());
            stmt.setString(4, message.getContent());
            stmt.setString(5, message.getMessageType().toString());
            stmt.setString(6, message.getSentTime().toString());
            // Setting the value to null if reply_to_id is not set
            if(message.getReplyToId() != null) {
                stmt.setString(7, message.getReplyToId().toString());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }
            stmt.setInt(8, referenceOnly ? 1 : 0);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error inserting message into local database " + e.getMessage());
            return false;
        }
    }

    // Insert all message statuses of a message dto into the database
    public boolean insertMessageStatuses(MessageDTO messageDTO){
        String insertString = "INSERT INTO message_status(id, message_id, user_id, status) VALUES (?, ?, ?, ?)";
        try(PreparedStatement stmt = connection.prepareStatement(insertString)){
            // Iterate through message status and for each message status create insert statement and batch it
            for(MessageStatusDTO messageStatus : messageDTO.getStatuses()){
                stmt.setString(1, messageStatus.getId().toString());
                stmt.setString(2, messageDTO.getId().toString());
                stmt.setString(3, messageStatus.getUserId().toString());
                stmt.setString(4, messageStatus.getStatus().toString());
                stmt.addBatch();
            }
            return stmt.executeBatch().length == messageDTO.getStatuses().size();
        } catch (SQLException e) {
            System.out.println("Error inserting message statuses for message " + messageDTO.getId() + " into local database");
            return false;
        }
    }

    // Insert message status dto into the database
    public boolean insertMessageStatus(MessageStatusChangeDTO messageStatus){
        String insertString = "INSERT INTO message_status(id, message_id, user_id, status) VALUES (?, ?, ?, ?)";

        try(PreparedStatement stmt = connection.prepareStatement(insertString)){
            // Place all needed values into prepared statement
            stmt.setString(1, messageStatus.getId().toString());
            stmt.setString(2, messageStatus.getMessageId().toString());
            stmt.setString(3, messageStatus.getUserId().toString());
            stmt.setString(4, messageStatus.getStatus().toString());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error inserting message status into local database " + e.getMessage());
            return false;
        }
    }

    // Get messages from specified chat, only gets a page's worth of chat messages
    public ArrayList<MessageDTO> getChatMessages(UUID chatId, int pageNumber, int pageSize) {
        String queryString = "SELECT m.id, m.chat_id, m.content, m.message_type, m.sent_time, m.reply_to_id, u.id as user_id, u.username " +
                "FROM message m " +
                "JOIN user u ON m.sender_id = u.id " +
                "WHERE m.chat_id = ? " +
                "AND m.reference_only = 0 " +
                "ORDER BY sent_time DESC " +
                "LIMIT ? OFFSET ?";

        try(PreparedStatement stmt = connection.prepareStatement(queryString)){
            // Create map of messages that will be used for easier message lookup while loading message statuses for each message
            // (Using Linked hash map to retain message order so they can be displayed in chat properly)
            Map<UUID, MessageDTO> messagesMap = new LinkedHashMap<>();

            // Handle invalid values for page size and page number
            pageSize = pageSize < 0 ? 25 : pageSize;
            pageNumber = Math.max(pageNumber-1, 0);

            // Set prepared statement parameters
            stmt.setString(1, chatId.toString());
            stmt.setInt(2, pageSize);
            stmt.setInt(3, pageNumber * pageSize);

            ResultSet queryResult = stmt.executeQuery();
            while(queryResult.next()){
                // Create message dto without statuses
                MessageDTO message = MessageDTO.builder()
                        .id(UUID.fromString(queryResult.getString("id")))
                        .chatId(UUID.fromString(queryResult.getString("chat_id")))
                        .sender(UserDTO.builder()
                                .id(UUID.fromString(queryResult.getString("user_id")))
                                .username(queryResult.getString("username"))
                                .build())
                        .content(queryResult.getString("content"))
                        .messageType(MessageType.valueOf(queryResult.getString("message_type")))
                        .sentTime(Instant.parse(queryResult.getString("sent_time")))
                        .replyToId(queryResult.getString("reply_to_id") == null ? null : UUID.fromString(queryResult.getString("reply_to_id")))
                        .build();

                // Add the message dto to the messages map
                messagesMap.put(message.getId(), message);
            }

            if(!messagesMap.isEmpty()) {
                // Get message statuses for all messages in the map
                setMessagesStatuses(messagesMap);
            }

            // Return the collection of map values as list
            return new ArrayList<>(messagesMap.values());
        } catch (SQLException e) {
            System.out.println("Error getting message objects for specified chat from the local database " + e.getMessage());
            return null;
        }
    }

    // Sets the message status for the message objects in the message map
    private void setMessagesStatuses(Map<UUID, MessageDTO> messageMap) {
        // Create the query string and add the number of ? equal to the size of the message map to be used for "IN" clause
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT id, message_id, user_id, status " +
                "FROM message_status " +
                "WHERE message_id IN (");
        // Count the number of messages which have 1 or more message statuses
        queryBuilder.append("?,".repeat(messageMap.size()));
        queryBuilder.replace(queryBuilder.length()-1, queryBuilder.length(), ")");

        // Create statement and place each uuid in the appropriate query parameter
        try(PreparedStatement stmt = connection.prepareStatement(queryBuilder.toString())){
            int counter = 1;
            for(UUID key: messageMap.keySet()){
                stmt.setString(counter, key.toString());
                counter++;
            }
            ResultSet queryResult = stmt.executeQuery();
            // Iterate through the results and add each status to appropriate message
            while(queryResult.next()){
                // Get the message id and the message status dto from the row
                UUID messageID = UUID.fromString(queryResult.getString("message_id"));
                MessageStatusDTO messageStatus = MessageStatusDTO.builder()
                        .id(UUID.fromString(queryResult.getString("id")))
                        .userId(UUID.fromString(queryResult.getString("user_id")))
                        .status(StatusType.valueOf(queryResult.getString("status")))
                        .build();

                // Add the message status to appropriate message statuses list
                messageMap.get(messageID).getStatuses().add(messageStatus);
            }
        } catch (SQLException e) {
            System.out.println("Error getting message status objects for the specified messages from the local database " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    // Finds the message by id
    public MessageDTO findMessageById(UUID messageId){
        String queryString = "SELECT m.id AS message_id, m.chat_id, m.sender_id, m.content, m.message_type, m.sent_time, m.reply_to_id, " +
                "ms.id AS status_id, ms.user_id, ms.status, u.username FROM message m " +
                "LEFT JOIN message_status ms ON ms.message_id = m.id " +
                "JOIN user u ON u.id = m.sender_id " +
                "WHERE m.id = ? ";

        try(PreparedStatement stmt = connection.prepareStatement(queryString)) {
            // Set prepared statement parameter
            stmt.setString(1, messageId.toString());

            ResultSet rs = stmt.executeQuery();
            MessageDTO result = null;
            while(rs.next()) {
                if(result == null) {
                    result = MessageDTO.builder()
                            .id(UUID.fromString(rs.getString("message_id")))
                            .chatId(UUID.fromString(rs.getString("chat_id")))
                            .sender(UserDTO.builder()
                                    .id(UUID.fromString(rs.getString("sender_id")))
                                    .username(rs.getString("username"))
                                    .build())
                            .content(rs.getString("content"))
                            .replyToId(rs.getString("reply_to_id") == null ? null : UUID.fromString(rs.getString("reply_to_id")))
                            .messageType(MessageType.valueOf(rs.getString("message_type")))
                            .sentTime(Instant.parse(rs.getString("sent_time")))
                            .build();

                    // Check if a single user status exists, only in this case can a null status be joined
                    if(rs.getString("status_id") == null) {
                        return result;
                    }
                }

                // Create message status dto object and add it to the message dto
                MessageStatusDTO messageStatus = MessageStatusDTO.builder()
                        .id(UUID.fromString(rs.getString("status_id")))
                        .userId(UUID.fromString(rs.getString("user_id")))
                        .status(StatusType.valueOf(rs.getString("status")))
                        .build();

                result.getStatuses().add(messageStatus);
            }

            return result;
        } catch (SQLException e) {
            System.out.println("Error getting message for message - " + messageId  + " from the local database " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Returns a list of message which have an id from the given message uuid list
    public List<MessageDTO> findMessagesByIdIn(List<UUID> messageIds){
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT m.id AS message_id, m.chat_id, m.sender_id, m.content, m.message_type, m.sent_time, m.reply_to_id, " +
                "ms.id AS status_id, ms.user_id, ms.status, u.username FROM message m " +
                "LEFT JOIN message_status ms ON ms.message_id = m.id " +
                "JOIN user u ON u.id = m.sender_id " +
                "WHERE m.id IN (");
        queryBuilder.append("?,".repeat(messageIds.size()));
        queryBuilder.replace(queryBuilder.length()-1, queryBuilder.length(), ")");

        try(PreparedStatement stmt = connection.prepareStatement(queryBuilder.toString())) {
            // Set prepared statement parameters
            // Go through to message ids and place each id instead of a parameter marker in the statement
            for(int i = 1; i<=messageIds.size();i++){
                stmt.setString(i, messageIds.get(i-1).toString());
            }

            ResultSet rs = stmt.executeQuery();
            Map<UUID, MessageDTO> result = new HashMap<>();
            while(rs.next()) {
                UUID messageId = UUID.fromString(rs.getString("message_id"));
                // Check if the message dto is already in the map, if it isn't create the message dto and add it to the map
                if(!result.containsKey(messageId)) {
                    MessageDTO message  = MessageDTO.builder()
                            .id(UUID.fromString(rs.getString("message_id")))
                            .chatId(UUID.fromString(rs.getString("chat_id")))
                            .sender(UserDTO.builder()
                                    .id(UUID.fromString(rs.getString("sender_id")))
                                    .username(rs.getString("username"))
                                    .build())
                            .content(rs.getString("content"))
                            .replyToId(rs.getString("reply_to_id") == null ? null : UUID.fromString(rs.getString("reply_to_id")))
                            .messageType(MessageType.valueOf(rs.getString("message_type")))
                            .sentTime(Instant.parse(rs.getString("sent_time")))
                            .build();
                    result.put(messageId, message);

                    // Check if a single user status exists, only in this case can a null status be joined
                    if(rs.getString("status_id") == null) {
                        continue;
                    }
                }

                // Create message status dto object and add it to the message dto
                MessageStatusDTO messageStatus = MessageStatusDTO.builder()
                        .id(UUID.fromString(rs.getString("status_id")))
                        .userId(UUID.fromString(rs.getString("user_id")))
                        .status(StatusType.valueOf(rs.getString("status")))
                        .build();
                result.get(messageId).getStatuses().add(messageStatus);
            }

            return result.values().stream().toList();
        } catch (SQLException e) {
            System.out.println("Error getting messages for given message id list from the local database " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    // Updates the status of existing message status
    public boolean updateMessageStatus(MessageStatusChangeDTO statusChangeDTO) {
        String updateQuery = "UPDATE message_status SET status = ? WHERE user_id = ? AND message_id = ?";

        try(PreparedStatement stmt = connection.prepareStatement(updateQuery)){
            // Place all needed values into the prepared statement
            stmt.setString(1, statusChangeDTO.getStatus().toString());
            stmt.setString(2, statusChangeDTO.getUserId().toString());
            stmt.setString(3, statusChangeDTO.getMessageId().toString());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating the message status for the message " + statusChangeDTO.getMessageId() +
                    ", and user " + statusChangeDTO.getUserId() + "\nError message: " + e.getMessage());
            return false;
        }
    }

    // Updates all messages' statuses in the chat for that user to seen
    public boolean bulkSeenUpdate(UUID userId, UUID chatId) {
        String updateQuery = "UPDATE message_status SET status = 'SEEN' " +
                "WHERE user_id = ? AND message_id IN (SELECT id FROM message WHERE chat_id = ?)";

        try(PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
            // Place all needed values into the prepared statement
            stmt.setString(1, userId.toString());
            stmt.setString(2, chatId.toString());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error bulk setting the status to SEEN for messages in chat - " + chatId
                    + ", for user - " + userId + ", error message: " + e.getMessage());
            return false;
        }
    }

    // Updates all messages' statuses provided for the user to delivered
    public boolean bulkDeliveredUpdate(UUID userId, List<UUID> messageIds) {
        // Create the query string and add the number of ? equal to the size of the message list to be used for "IN" clause
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("UPDATE message_status " +
                "SET status = 'DELIVERED' " +
                "WHERE user_id = ? " +
                "AND message_id IN (");
        queryBuilder.append("?,".repeat(messageIds.size()));
        queryBuilder.replace(queryBuilder.length()-1, queryBuilder.length(), ")");

        // Create statement and place each uuid in the appropriate query parameter
        try(PreparedStatement stmt = connection.prepareStatement(queryBuilder.toString())){
            stmt.setString(1, userId.toString());
            int counter = 2;
            // Go through to message ids and place each id instead of a parameter marker in the statement
            for(UUID messageId: messageIds){
                stmt.setString(counter, messageId.toString());
                counter++;
            }
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error bulk setting the status to DELIVERED for messages for user - " + userId + ", error message: " + e.getMessage());
            return false;
        }
    }

    // Get the oldest sent time for messages of a specific chat
    public Instant getOldestSentTimeChatMsg(UUID chatId) {
        String queryString = "SELECT sent_time FROM message WHERE chat_id = ? AND reference_only = 0 ORDER BY sent_time LIMIT 1";

        try(PreparedStatement stmt = connection.prepareStatement(queryString)) {
            stmt.setString(1, chatId.toString());
            ResultSet rs = stmt.executeQuery();
            if(rs.next()) {
                return Instant.parse(rs.getString("sent_time"));
            }
        } catch (SQLException e) {
            System.out.println("Error getting latest sent time for messages in chat - " + chatId + ", error message: " + e.getMessage());
        }
        return null;
    }

    // Returns message statuses for each message
    public List<MessageStatusChangeDTO> getStatusesForMessages(Set<UUID> messageIds) {
        // Create the query string and add the number of ? equal to the size of the message map to be used for "IN" clause
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT id, message_id, user_id, status " +
                "FROM message_status " +
                "WHERE message_id IN (");
        // Count the number of messages in the  messageIds set and append the appropriate number of ?
        queryBuilder.append("?,".repeat(messageIds.size()));
        queryBuilder.replace(queryBuilder.length()-1, queryBuilder.length(), ")");

        // Create statement and place each uuid in the appropriate query parameter
        try(PreparedStatement stmt = connection.prepareStatement(queryBuilder.toString())){
            int counter = 1;
            for(UUID messageId: messageIds){
                stmt.setString(counter, messageId.toString());
                counter++;
            }
            ResultSet rs = stmt.executeQuery();
            Map<UUID, MessageStatusChangeDTO> updatedStatuses = new HashMap<>();
            // Go through the result set and update the map
            while(rs.next()) {
                // Create message status change dto from the message status data
                MessageStatusChangeDTO statusDTO = MessageStatusChangeDTO.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .messageId(UUID.fromString(rs.getString("message_id")))
                        .userId(UUID.fromString(rs.getString("user_id")))
                        .status(StatusType.valueOf(rs.getString("status")))
                        .build();

                // If message is not in the updatedStatuses map add it and it's status automatically
                if(!updatedStatuses.containsKey(statusDTO.getMessageId())){
                    updatedStatuses.put(statusDTO.getMessageId(), statusDTO);
                    continue;
                }

                // If the status in the map is SEEN and the current status in result set is DELIVERED OR SEEN, or if the
                // status in the map is DELIVERED and current is SEEN change the value of the status in the map with current
                if(!updatedStatuses.get(statusDTO.getMessageId()).getStatus().equals(statusDTO.getStatus())) {
                    if(updatedStatuses.get(statusDTO.getMessageId()).getStatus().equals(StatusType.SENT) ||
                            updatedStatuses.get(statusDTO.getMessageId()).getStatus().equals(StatusType.DELIVERED) && statusDTO.getStatus().equals(StatusType.SEEN)){
                        updatedStatuses.put(statusDTO.getMessageId(), statusDTO);
                    }
                }
            }

            // Return the list of values which represents the current highest status for each message, meaning that each
            // message that has a single seen is marked as seen, or if it has sent and delivered statuses it's marked as delivered...
            return updatedStatuses.values().stream().toList();

        } catch (SQLException e) {
            System.out.println("Error getting message status for the given list of messages, error message: " + e.getMessage());
            return null;
        }
    }

    // Returns message statuses for the given message, together with username of the user. Needed for displaying users
    // which seen/received a message
    public List<MessageStatusDisplay> getMessageStatuses(UUID messageId) {
        String queryString = "SELECT ms.id, ms.user_id, ms.status, u.username " +
                "FROM message_status ms " +
                "JOIN user u ON u.id = ms.user_id " +
                "WHERE ms.message_id = ?";

        try(PreparedStatement stmt = connection.prepareStatement(queryString)){
            stmt.setString(1,messageId.toString());
            List<MessageStatusDisplay> statuses = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();

            // Go through the result set and add the statuses to the list
            while(rs.next()){
                UserDTO user = UserDTO.builder()
                        .id(UUID.fromString(rs.getString("user_id")))
                        .username(rs.getString("username"))
                        .build();
                MessageStatusDisplay status = new MessageStatusDisplay(
                        user,
                        StatusType.valueOf(rs.getString("status")),
                        UUID.fromString(rs.getString("id")));

                statuses.add(status);
            }

            // Return the list of statuses
            return statuses;
        } catch (SQLException e) {
            System.out.println("Error getting message statuses for message with id: " + messageId + ", error message: " + e.getMessage());
            return null;
        }
    }

    // Delete the content from message with given id
    public void deleteMessageContent(UUID messageId){
        String updateString = "UPDATE message " +
                "SET content = NULL " +
                "WHERE id = ?";

        try(PreparedStatement stmt = connection.prepareStatement(updateString))
        {
            // Place all needed values into prepared statement
            stmt.setString(1, messageId.toString());

            // Execute statement
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error erasing message content from local database " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // This method updates the replyToId for given message
    public void updateMessageReplyToId(UUID messageId, UUID replyToId) {
        String updateString = "UPDATE message " +
                "SET reply_to_id = ? " +
                "WHERE id = ?";
        try(PreparedStatement stmt = connection.prepareStatement(updateString))
        {
            // Place all needed values into prepared statement
            stmt.setString(1, replyToId.toString());
            stmt.setString(2, messageId.toString());

            // Execute statement
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating replyToId for message " + messageId + " local database " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
