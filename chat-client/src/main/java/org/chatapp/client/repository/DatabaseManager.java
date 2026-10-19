package org.chatapp.client.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;

public class DatabaseManager {

    private final Connection connection;

    public DatabaseManager(String url) throws SQLException {
        connection = DriverManager.getConnection(url);
        databaseInitialization();
    }

    // Method that takes in the statements to be executed and runs them inside a transaction
    public synchronized void executeTransaction(Runnable operation) {
        try {
            connection.setAutoCommit(false);
            operation.run();
            connection.commit();
        } catch (Exception e) {
            System.out.println("Error executing the transaction in method: " + e.getMessage() + ", rolling back the transaction...");
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                System.out.println("Error occurred trying to rollback transaction, error message: " + e.getMessage());
            }
            throw new RuntimeException(e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Error occurred turning back on auto commit, error message: " + e.getMessage());
            }
        }
    }

    // Method that takes in the query to be executed,runs it inside a transaction and returns the result
    public synchronized <T> T executeQuery(Callable<T> query) {
        try {
            return query.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Database initialization method, creates tables if they don't exist
    private void databaseInitialization() throws SQLException {
        generalDbSetup();
        createChatTable();
        createUserTable();
        createMessageTable();
        createMessageStatusTable();
        createChatMemberTable();
        createSyncLogTable();
        createStatusSyncLogTable();
        createIndexes();
    }

    // Enables foreign key checks
    private void generalDbSetup() throws SQLException{
        String sql = """
                PRAGMA foreign_keys = ON
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // Creates the chat table
    private void createChatTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS chat(
                    id TEXT PRIMARY KEY,
                    chat_name TEXT,
                    chat_type TEXT NOT NULL,
                    last_message_content TEXT,
                    last_message_time TEXT,
                    unread_count INTEGER
                )
                """;

        try (Statement stmt = connection.createStatement()){
            stmt.execute(sql);
        }
    }

    // Creates the chat_member table
    private void createChatMemberTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS chat_member(
                    id TEXT primary key,
                    user_id TEXT NOT NULL,
                    chat_id TEXT NOT NULL,
                    muted_until TEXT,
                    blocked INTEGER,
                    pinned INTEGER,
                    role TEXT,
                    FOREIGN KEY (user_id) REFERENCES user(id),
                    FOREIGN KEY (chat_id) REFERENCES chat(id),
                    UNIQUE(user_id, chat_id)
                )
                """;

        try (Statement stmt = connection.createStatement()){
            stmt.execute(sql);
        }
    }

    // Creates the message table
    private void createMessageTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS message(
                    id TEXT PRIMARY KEY,
                    chat_id TEXT NOT NULL,
                    sender_id TEXT NOT NULL,
                    content TEXT,
                    message_type TEXT NOT NULL,
                    sent_time TEXT NOT NULL,
                    reply_to_id TEXT,
                    reference_only INTEGER DEFAULT 0 NOT NULL,
                    FOREIGN KEY (chat_id) REFERENCES chat(id),
                    FOREIGN KEY (sender_id) REFERENCES user(id),
                    FOREIGN KEY (reply_to_id) REFERENCES message(id)
                )
                """;

        try (Statement stmt = connection.createStatement()){
            stmt.execute(sql);
        }
    }

    // Creates the message_status table
    private void createMessageStatusTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS message_status(
                    id TEXT PRIMARY KEY,
                    message_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    status TEXT NOT NULL,
                    FOREIGN KEY (message_id) REFERENCES message(id),
                    FOREIGN KEY (user_id) REFERENCES user(id),
                    UNIQUE(message_id, user_id)
                )
                """;

        try(Statement stmt = connection.createStatement()){
            stmt.execute(sql);
        }
    }

    // Creates the user table
    private void createUserTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS user(
                    id TEXT PRIMARY KEY,
                    username TEXT NOT NULL UNIQUE
                )
                """;

        try(Statement stmt = connection.createStatement()){
            stmt.execute(sql);
        }
    }

    // Creates the table which tracks when the last sync with remote database was performed
    private void createSyncLogTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS sync(
                    id TEXT PRIMARY KEY,
                    update_time TEXT NOT NULL
                )
                """;

        try(Statement stmt = connection.createStatement()){
            stmt.execute(sql);
        }
    }

    // Creates the table which tracks when the last message status sync with remote database for a given chat was performed
    private void createStatusSyncLogTable() throws SQLException{
        String sql = """
                CREATE TABLE IF NOT EXISTS status_sync(
                    id TEXT PRIMARY KEY,
                    chat_id TEXT NOT NULL,
                    update_time TEXT NOT NULL,
                    FOREIGN KEY (chat_id) REFERENCES chat(id)
                )
                """;

        try(Statement stmt = connection.createStatement()){
            stmt.execute(sql);
        }
    }

    private void createIndexes() throws SQLException {
        String sql1 = "CREATE UNIQUE INDEX IF NOT EXISTS idx_msg_status_user_msg ON message_status(user_id, message_id)";
        String sql2 = "CREATE INDEX IF NOT EXISTS idx_message_chat_id ON message(chat_id)";
        String sql3 = "CREATE INDEX IF NOT EXISTS idx_chat_member_user_id ON chat_member(user_id)";

        try(Statement stmt = connection.createStatement()){
            // Turn off auto-commit for batch execution
            connection.setAutoCommit(false);

            stmt.addBatch(sql1);
            stmt.addBatch(sql2);
            stmt.addBatch(sql3);
            stmt.executeBatch();

            connection.commit();
            connection.setAutoCommit(true);
        }
    }

    public Connection getConnection() {
        return connection;
    }

}
