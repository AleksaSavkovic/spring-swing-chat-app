package org.chatapp.client.repository;

import org.chatapp.common.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class ChatRepository {

    private final Connection connection;

    public ChatRepository(DatabaseManager databaseManager) {
        this.connection = databaseManager.getConnection();
    }

    // Inserts chat dto into the local database
    public boolean insertChat(ChatDTO chat) {
        String insertString = "INSERT INTO chat(id, chat_name, chat_type) VALUES (?, ?, ?)";
        String insertChatMemberString = "INSERT INTO chat_member(id, user_id, chat_id, muted_until, blocked, pinned, role) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?)";
        String insertUserString = "INSERT OR IGNORE INTO user(id, username) VALUES(?, ?)";
        try(PreparedStatement stmt = connection.prepareStatement(insertString);
            PreparedStatement cmemberStmt = connection.prepareStatement(insertChatMemberString);
            PreparedStatement userStmt = connection.prepareStatement(insertUserString)
        ){
            // Place all needed values into prepared statement
            stmt.setString(1, chat.getId().toString());
            stmt.setString(2, chat.getChatName());
            stmt.setString(3, chat.getChatType().toString());

            // Add all the chats chat members and users(if not already in database)
            for(ChatMemberDTO chatMember : chat.getUsers()){
                // Batching the user object insert
                userStmt.setString(1, chatMember.getUserId().toString());
                userStmt.setString(2, chatMember.getUsername());
                userStmt.addBatch();

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

            int created = stmt.executeUpdate();
            userStmt.executeBatch();
            cmemberStmt.executeBatch();
            return created > 0;
        } catch (SQLException e) {
            System.out.println("Error inserting chat dto into local database " + e.getMessage());
            return false;
        }
    }

    // Get a list of all chats of which a user is member of
    public List<ChatDTO> getUserChatsWithoutMembers(UUID activeUser){
        String queryString = "SELECT c.id, c.chat_name, c.chat_type, c.last_message_content, c.last_message_time, c.unread_count " +
                "FROM chat c " +
                "JOIN chat_member cm on c.id = cm.chat_id " +
                "WHERE cm.user_id = ?";

        try(PreparedStatement stmt = connection.prepareStatement(queryString)){
            // Create list to be returned and set the prepared statement parameter
            List<ChatDTO> chats = new ArrayList<>();
            stmt.setString(1, activeUser.toString());

            // Execute the query
            ResultSet queryResult = stmt.executeQuery();

            // Iterate through result set and fill chat list
            while(queryResult.next()){
                // Create a chat dto from a result set row and add it to the list of dtos to be returned
                ChatDTO chat = ChatDTO.builder()
                        .id(UUID.fromString(queryResult.getString("id")))
                        .chatName(queryResult.getString("chat_name"))
                        .chatType(ChatType.valueOf(queryResult.getString("chat_type")))
                        .lastMessageContent(queryResult.getString("last_message_content"))
                        .lastMessageSentTime(queryResult.getString("last_message_time") == null ? null : Instant.parse(queryResult.getString("last_message_time")))
                        .unreadMessages(queryResult.getInt("unread_count"))
                        .build();

                chats.add(chat);
            }

            return chats;
        } catch (SQLException e) {
            System.out.println("Error getting chat objects for specified user from the local database " + e.getMessage());
            return null;
        }

    }

    // Get a list of all chats, along with their members, of which a user is member of
    public List<ChatDTO> getUserChats(UUID activeUser) {
        String queryString = "SELECT c.id, c.chat_name, c.chat_type, c.last_message_content, c.last_message_time, c.unread_count, " +
                "cm.id as cm_id, cm.muted_until, cm.pinned, cm.blocked, cm.role, cm.user_id, u.username " +
                "FROM chat c " +
                "JOIN chat_member cm ON c.id = cm.chat_id " +
                "JOIN user u ON cm.user_id = u.id " +
                "WHERE c.id IN (SELECT chat_id FROM chat_member WHERE user_id = ?)";


        // Sub query for seeing chats of which a user is member of
        try(PreparedStatement stmt = connection.prepareStatement(queryString)){
            // Create list to be returned and set the prepared statement parameter
            Map<UUID, ChatDTO> chats = new HashMap<>();
            stmt.setString(1, activeUser.toString());

            // Execute the query
            ResultSet queryResult = stmt.executeQuery();

            // Iterate through result set and fill chat list
            while(queryResult.next()){

                UUID chatId = UUID.fromString(queryResult.getString("id"));

                // Check if chat is already in the map, if not add it to the map
                if(!chats.containsKey(chatId)) {
                    ChatDTO chat = ChatDTO.builder()
                            .id(UUID.fromString(queryResult.getString("id")))
                            .chatName(queryResult.getString("chat_name"))
                            .chatType(ChatType.valueOf(queryResult.getString("chat_type")))
                            .lastMessageContent(queryResult.getString("last_message_content"))
                            .lastMessageSentTime(queryResult.getString("last_message_time") == null ? null : Instant.parse(queryResult.getString("last_message_time")))
                            .unreadMessages(queryResult.getInt("unread_count"))
                            .build();

                    chats.put(chat.getId(), chat);
                }

                // Creating the chat member dto from the result set data
                ChatMemberDTO chatMemberDTO = ChatMemberDTO.builder()
                        .id(UUID.fromString(queryResult.getString("cm_id")))
                        .userId(UUID.fromString(queryResult.getString("user_id")))
                        .username(queryResult.getString("username"))
                        .mutedUntil(queryResult.getString("muted_until") == null ? null : Instant.parse(queryResult.getString("muted_until")))
                        .pinned(queryResult.getBoolean("pinned"))
                        .blocked(queryResult.getBoolean("blocked"))
                        .role(ChatterRole.valueOf(queryResult.getString("role")))
                        .build();

                // Add the chat member dto to the chats map
                chats.get(chatId).getUsers().add(chatMemberDTO);
            }

            // Return all the chat dtos contained in the map
            return new ArrayList<>(chats.values());
        } catch (SQLException e) {
            System.out.println("Error getting chat objects with chat members for specified user from the local database " + e.getMessage());
            return null;
        }
    }


    // Find chat in local database
    public ChatDTO findChatById(UUID chatId) {
        String queryString = "SELECT * FROM chat WHERE id = ?";

        try(PreparedStatement stmt = connection.prepareStatement(queryString)) {
            // Place all needed values into prepared statement
            stmt.setString(1, chatId.toString());

            ResultSet queryResult = stmt.executeQuery();
            if(queryResult.next()) {
                return ChatDTO.builder()
                        .id(UUID.fromString(queryResult.getString("id")))
                        .chatName(queryResult.getString("chat_name"))
                        .chatType(ChatType.valueOf(queryResult.getString("chat_type")))
                        .lastMessageContent(queryResult.getString("last_message_content"))
                        .lastMessageSentTime(queryResult.getString("last_message_time") == null ? null : Instant.parse(queryResult.getString("last_message_time")))
                        .unreadMessages(queryResult.getInt("unread_count"))
                        .build();
            }
            return null;
        } catch (SQLException e) {
            System.out.println("Error getting chat with id - " + chatId + " from the local database, error message: " + e.getMessage());
            return null;
        }
    }

    // Get a list of all chats, along with their members, of which a user is member of
    public ChatDTO findChatByIdWithUsers(UUID chatId) {
        String queryString = "SELECT c.id, c.chat_name, c.chat_type, c.last_message_content, c.last_message_time, c.unread_count, " +
                "cm.id as cm_id, cm.muted_until, cm.pinned, cm.blocked, cm.role, cm.user_id, u.username " +
                "FROM chat c " +
                "JOIN chat_member cm ON c.id = cm.chat_id " +
                "JOIN user u ON cm.user_id = u.id " +
                "WHERE c.id = ?";


        // Sub query for seeing chats of which a user is member of
        try(PreparedStatement stmt = connection.prepareStatement(queryString)){
            // Set the prepared statement parameter and execute the query
            stmt.setString(1, chatId.toString());
            ResultSet queryResult = stmt.executeQuery();

            // Return all the chat containing chat members
            return createChatWithMembersFromQueryResult(queryResult);
        } catch (SQLException e) {
            System.out.println("Error getting chat objects with chat members for specified user from the local database " + e.getMessage());
            return null;
        }
    }

    // Find a chat and the chat members for a regular chat which has both users
    public ChatDTO findChatByUsers(UUID user1Id, UUID user2Id) {
        String queryString = "SELECT c.id, c.chat_name, c.chat_type, c.last_message_content, c.last_message_time, c.unread_count, " +
                "cm.id as cm_id, cm.muted_until, cm.pinned, cm.blocked, cm.role, cm.user_id, u.username " +
                "FROM chat c " +
                "JOIN chat_member cm ON c.id = cm.chat_id " +
                "JOIN user u ON cm.user_id = u.id " +
                "WHERE c.chat_type = 'REGULAR' " +
                "AND c.id IN (SELECT chat_id FROM chat_member WHERE user_id = ? OR user_id = ? " +
                "GROUP BY chat_id " +
                "HAVING count(*)=2)";

        // Sub query for seeing chats of which a user is member of
        try(PreparedStatement stmt = connection.prepareStatement(queryString)){
            // Set the prepared statement parameters and execute the query
            stmt.setString(1, user1Id.toString());
            stmt.setString(2, user2Id.toString());
            ResultSet queryResult = stmt.executeQuery();

            // Return all the chat containing chat members
            return createChatWithMembersFromQueryResult(queryResult);
        } catch (SQLException e) {
            System.out.println("Error getting chat with chat members for which has members with user ids: " + user1Id +
                    " and " + user2Id + " from the local database " + e.getMessage());
            return null;
        }
    }

    // Takes in a query result from a query that returns chats with chat member data and builds the chatDTO object
    private ChatDTO createChatWithMembersFromQueryResult(ResultSet queryResult) throws SQLException {
        // Create list to be returned and set the prepared statement parameter
        ChatDTO chat = null;
        // Iterate through result set and fill chat list
        while(queryResult.next()){
            // Check if the base chat dto hasn't been created yet, if not create it
            if(chat == null) {
                chat = ChatDTO.builder()
                        .id(UUID.fromString(queryResult.getString("id")))
                        .chatName(queryResult.getString("chat_name"))
                        .chatType(ChatType.valueOf(queryResult.getString("chat_type")))
                        .lastMessageContent(queryResult.getString("last_message_content"))
                        .lastMessageSentTime(queryResult.getString("last_message_time") == null ? null : Instant.parse(queryResult.getString("last_message_time")))
                        .unreadMessages(queryResult.getInt("unread_count"))
                        .build();
            }

            // Creating the chat member dto from the result set data
            ChatMemberDTO chatMemberDTO = ChatMemberDTO.builder()
                    .id(UUID.fromString(queryResult.getString("cm_id")))
                    .userId(UUID.fromString(queryResult.getString("user_id")))
                    .username(queryResult.getString("username"))
                    .mutedUntil(queryResult.getString("muted_until") == null ? null : Instant.parse(queryResult.getString("muted_until")))
                    .pinned(queryResult.getBoolean("pinned"))
                    .blocked(queryResult.getBoolean("blocked"))
                    .role(ChatterRole.valueOf(queryResult.getString("role")))
                    .build();

            // Add the chat member dto to the chats map
            chat.getUsers().add(chatMemberDTO);
        }
        return chat;
    }

    // Recalculates the unread messages count for the chat base on chat id and the user id
    public int recalculateChatUnreadCount(UUID chatId, UUID userId){
        String queryString = "SELECT count() as unread FROM message_status ms " +
                "JOIN message m ON ms.message_id = m.id " +
                "JOIN chat c ON m.chat_id = c.id " +
                "WHERE c.id = ? AND ms.user_id = ? AND ms.status <> 'SEEN'";

        try(PreparedStatement stmt = connection.prepareStatement(queryString)) {
            // Place all needed values into prepared statement
            stmt.setString(1, chatId.toString());
            stmt.setString(2, userId.toString());
            ResultSet queryResult = stmt.executeQuery();

            if(queryResult.next()) {
                return queryResult.getInt("unread");
            }
            return -1;
        } catch (SQLException e) {
            System.out.println("Error getting unread message count for user - " + userId + ", in chat - " + chatId +
                    " from the local database, error message: " + e.getMessage());
            return -1;
        }
    }

    // Update chat info - unread count and last message
    public boolean updateChatInfo(ChatDTO chat){
        String updateString = "UPDATE chat " +
                "SET last_message_content = ?, " +
                "last_message_time = ?, " +
                "unread_count = ? " +
                "WHERE id = ?";

        try(PreparedStatement stmt = connection.prepareStatement(updateString)){
            // Place all needed values into prepared statement
            stmt.setString(1, chat.getLastMessageContent());
            stmt.setString(2, chat.getLastMessageSentTime().toString());
            stmt.setInt(3, chat.getUnreadMessages());
            stmt.setString(4, chat.getId().toString());

            // Return true if more than 0 rows were affected with the update
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating chat info for chat " + chat.getId() +
                    " in the local database\nError message: " + e.getMessage());
            return false;
        }
    }

}
