package org.chatapp.client.repository;

import org.chatapp.common.model.UserDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ChatMemberRepository {

    private final Connection connection;

    public ChatMemberRepository(DatabaseManager databaseManager) {
        this.connection = databaseManager.getConnection();
    }

    // Inserts user dto into the local database
    public boolean insertUser(UserDTO user){
        String insertString = "INSERT INTO user(id, username) VALUES(?, ?)";

        try(PreparedStatement stmt = connection.prepareStatement(insertString)){
            // Place all needed values into the prepared statement
            stmt.setString(1, user.getId().toString());
            stmt.setString(2, user.getUsername());

            // Execute statement and return true if 1 or more rows has been changed
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error inserting user dto into local database " + e.getMessage());
            return false;
        }
    }

    // Find user in the local database by user id
    public UserDTO findUserById(UUID userId){
        String queryString = "SELECT id, username FROM user WHERE id = ?";

        try(PreparedStatement stmt = connection.prepareStatement(queryString)){
            // Place the user id value into the prepared statement
            stmt.setString(1, userId.toString());

            // Execute the query
            ResultSet queryResult = stmt.executeQuery();

            // Build the user dto from the found row and return it
            if(queryResult.next()){
                return UserDTO.builder()
                        .id(UUID.fromString(queryResult.getString("id")))
                        .username(queryResult.getString("username"))
                        .build();
            }
            System.out.println("No user found with the id " + userId);
            return null;
        } catch (SQLException e) {
            System.out.println("Error trying to find user with id " + userId + ", in the local database.\nError message: " + e.getMessage());
            return null;
        }
    }

}
