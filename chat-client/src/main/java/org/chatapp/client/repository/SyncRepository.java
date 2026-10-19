package org.chatapp.client.repository;

import org.chatapp.common.model.*;

import java.sql.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SyncRepository {
    private final Connection connection;

    public SyncRepository(DatabaseManager databaseManager) {
        this.connection = databaseManager.getConnection();
    }

    // Insert multiple chat dtos into the local database
    public boolean batchInsertChats(List<ChatDTO> chats) {
        // Create insert string for all statements that will be used
        String insertChatString = "INSERT INTO chat(id, chat_name, chat_type, last_message_content, last_message_time, unread_count) " +
                "VALUES (?, ?, ?, ?, ?, ?)" +
                "ON CONFLICT(id) DO UPDATE SET " +
                "chat_name = excluded.chat_name, chat_type = excluded.chat_type, last_message_content = excluded.last_message_content," +
                "last_message_time = excluded.last_message_time, unread_count = excluded.unread_count";
        String insertChatMemberString = "INSERT OR IGNORE INTO chat_member(id, user_id, chat_id, muted_until, blocked, pinned, role) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?)";
        String insertUserString = "INSERT OR IGNORE INTO user(id, username) VALUES(?, ?)";
        try(
                PreparedStatement chatStmt = connection.prepareStatement(insertChatString);
                PreparedStatement cmemberStmt = connection.prepareStatement(insertChatMemberString);
                PreparedStatement userStmt = connection.prepareStatement(insertUserString)
        ){
            // Initializing a set of users which will be used for user inserting to avoid inserting same user multiple times
            Set<UUID> insertedUsers = new HashSet<>();

            // Go through chats and batch each chat insert
            for(ChatDTO chat : chats) {
                // Place all needed values into prepared statement
                chatStmt.setString(1, chat.getId().toString());
                chatStmt.setString(2, chat.getChatName());
                chatStmt.setString(3, chat.getChatType().toString());
                chatStmt.setString(4, chat.getLastMessageContent() == null ? null : chat.getLastMessageContent());
                chatStmt.setString(5, chat.getLastMessageSentTime() ==
                        null ? null : chat.getLastMessageSentTime().toString());
                chatStmt.setInt(6, chat.getUnreadMessages());

                chatStmt.addBatch();

                // Go through chat members and batch each chat member and user
                for(ChatMemberDTO chatMember : chat.getUsers()){
                    // Batching the user object insert
                    if (!insertedUsers.contains(chatMember.getUserId())) {
                        userStmt.setString(1, chatMember.getUserId().toString());
                        userStmt.setString(2, chatMember.getUsername());
                        userStmt.addBatch();
                        insertedUsers.add(chatMember.getUserId());
                    }

                    // Batching the chat member object insert
                    cmemberStmt.setString(1, chatMember.getId().toString());
                    cmemberStmt.setString(2, chatMember.getUserId().toString());
                    cmemberStmt.setString(3, chat.getId().toString());
                    cmemberStmt.setString(4, chatMember.getMutedUntil() ==
                            null ? null : chatMember.getMutedUntil().toString());
                    cmemberStmt.setInt(5, chatMember.isBlocked() ? 1 : 0);
                    cmemberStmt.setInt(6, chatMember.isPinned() ? 1 : 0);
                    cmemberStmt.setString(7, chatMember.getRole().toString());
                    cmemberStmt.addBatch();
                }

            }
            // First execute the user batch, then the chat batch and lastly the chat member batch
            userStmt.executeBatch();
            chatStmt.executeBatch();
            cmemberStmt.executeBatch();

            return true;
        } catch (SQLException e) {
            System.out.println("Error batch inserting chat dto into local database " + e.getMessage());
            return false;
        }
    }

    // Inserts multiple message DTOs into the local database
    public boolean batchInsertMessage(List<MessageDTO> messages){
        String insertMessageString = "INSERT INTO message(id, chat_id, sender_id, content, message_type, sent_time, reply_to_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET " +
                "chat_id = excluded.chat_id, sender_id = excluded.sender_id, content = excluded.content, " +
                "message_type = excluded.message_type, sent_time = excluded.sent_time, reply_to_id = excluded.reply_to_id, reference_only = 0";

        String insertMessageStatusString = "INSERT OR IGNORE INTO message_status(id, message_id, user_id, status) VALUES (?, ?, ?, ?)";

        try(
                PreparedStatement msgStmt = connection.prepareStatement(insertMessageString);
                PreparedStatement mstatStmt = connection.prepareStatement(insertMessageStatusString)
        ){
            // Iterate through message list
            for(MessageDTO message : messages){
                // Place all needed values into prepared statement
                msgStmt.setString(1, message.getId().toString());
                msgStmt.setString(2, message.getChatId().toString());
                msgStmt.setString(3, message.getSender().getId().toString());
                msgStmt.setString(4, message.getContent());
                msgStmt.setString(5, message.getMessageType().toString());
                msgStmt.setString(6, message.getSentTime().toString());
                // Setting the value to null if reply_to_id is not set
                if(message.getReplyToId() != null) {
                    msgStmt.setString(7, message.getReplyToId().toString());
                } else {
                    msgStmt.setNull(7, Types.VARCHAR);
                }
                msgStmt.addBatch();

                for(MessageStatusDTO messageStatus : message.getStatuses()){
                    mstatStmt.setString(1, messageStatus.getId().toString());
                    mstatStmt.setString(2, message.getId().toString());
                    mstatStmt.setString(3, messageStatus.getUserId().toString());
                    mstatStmt.setString(4, messageStatus.getStatus().toString());
                    mstatStmt.addBatch();
                }
            }
            // First execute the message batch
            msgStmt.executeBatch();

            // Then execute the message statuses batch
            mstatStmt.executeBatch();

            return true;
        } catch (SQLException e) {
            System.out.println("Error batch inserting messages into local database " + e.getMessage());
            throw new RuntimeException(e);
//            return false;
        }
    }

    // Inserts multiple reference only MessageDTOs
    public boolean batchInsertReferenceOnlyMessage(List<MessageDTO> messages){
        String insertMessageString = "INSERT INTO message(id, chat_id, sender_id, content, message_type, sent_time, reference_only) " +
                "VALUES (?, ?, ?, ?, ?, ?, 1) ON CONFLICT(id) DO NOTHING";

        try(
                PreparedStatement msgStmt = connection.prepareStatement(insertMessageString)
        ){
            // Iterate through message list
            for(MessageDTO message : messages){
                // Place all needed values into prepared statement
                msgStmt.setString(1, message.getId().toString());
                msgStmt.setString(2, message.getChatId().toString());
                msgStmt.setString(3, message.getSender().getId().toString());
                msgStmt.setString(4, message.getContent());
                msgStmt.setString(5, message.getMessageType().toString());
                msgStmt.setString(6, message.getSentTime().toString());

                msgStmt.addBatch();
            }

            // First execute the message batch
            msgStmt.executeBatch();
            return true;
        } catch (SQLException e) {
            System.out.println("Error batch inserting reference only messages into local database " + e.getMessage());
            return false;
        }
    }


    // This method inserts or updates multiple message statuses
    public boolean batchUpdateStatuses(List<MessageStatusChangeDTO> statusUpdates){
        String upsertString =  "INSERT INTO message_status(id, message_id, user_id, status)" +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET " +
                "message_id = excluded.message_id, user_id = excluded.user_id, status = excluded.status";

        try(PreparedStatement stmt = connection.prepareStatement(upsertString)) {
            // Start transaction
            // Iterate through the status list
            for(MessageStatusChangeDTO statusUpdate : statusUpdates) {
                // Update the message
                stmt.setString(1, statusUpdate.getId().toString());
                stmt.setString(2, statusUpdate.getMessageId().toString());
                stmt.setString(3, statusUpdate.getUserId().toString());
                stmt.setString(4, statusUpdate.getStatus().toString());

                stmt.addBatch();
            }

            // Execute the sql statement
            stmt.executeBatch();
            return true;
        } catch (SQLException e) {
            System.out.println("Error batch inserting message statuses into the local database, error message: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Method that inserts the last sync time into the database
    public boolean insertSync(Instant lastSync){
        String insertString = "INSERT INTO sync(id, update_time) VALUES (?, ?)";

        try(PreparedStatement stmt = connection.prepareStatement(insertString)){
            // Place all needed values into prepared statement
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, lastSync.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error inserting sync data into local database " + e.getMessage());
            return false;
        }
    }

    // Returns the last sync(update time) for the active user, or null if there were no syncs
    public Instant getLastSync(){
        String queryString = "SELECT * FROM sync ORDER BY update_time DESC LIMIT 1";

        try(Statement stmt = connection.createStatement()){
            ResultSet queryResult = stmt.executeQuery(queryString);
            if(queryResult.next()){
                String timestampString = queryResult.getString("update_time");
                return Instant.parse(timestampString);
            } else {
                return null;
            }
        } catch (SQLException e) {
            System.out.println("Error getting the latest sync time " + e.getMessage());
            return null;
        }
    }

    // Method that inserts the last status sync time for given chat into the database
    public boolean insertStatusSync(UUID chatId, Instant lastSync) {
        String insertString = "INSERT INTO status_sync(id, chat_id, update_time) VALUES (?, ?, ?)";

        try(PreparedStatement stmt = connection.prepareStatement(insertString)){
            // Place all needed values into prepared statement
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, chatId.toString());
            stmt.setString(3, lastSync.toString());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error inserting status sync data into local database, error message: " + e.getMessage());
            return false;
        }
    }

    // Returns the last status_sync(update time) for the given chat, or null if there were no syncs
    public Instant getLastStatusSync(UUID chatId) {
        String queryString = "SELECT update_time FROM status_sync WHERE chat_id = ? ORDER BY update_time DESC LIMIT 1";

        try(PreparedStatement stmt = connection.prepareStatement(queryString)) {
            // Place the chat id value into the prepared statement
            stmt.setString(1,chatId.toString());
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return Instant.parse(rs.getString("update_time"));
            }
            return null;
        } catch (SQLException e) {
            System.out.println("Error getting last status sync for chat " + chatId + " from local database, error message: " + e.getMessage());
            return null;
        }
    }
}
