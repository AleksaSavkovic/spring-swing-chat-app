package org.chatapp.client.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.chatapp.client.repository.*;
import org.chatapp.client.service.*;
import org.chatapp.client.service.security.ClientTls;
import org.chatapp.common.model.AuthRequestDTO;
import org.chatapp.common.model.UserDTO;
import org.yaml.snakeyaml.Yaml;

import javax.net.ssl.SSLContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

public class ChatClientGui extends JFrame{

    public static final String HTTP_SERVER_URL;
    public static final String SOCKET_SERVER_URL;
    public static Path LOCAL_FILES_PATH;
    public static final int SOCKET_SERVER_PORT;
    public static final boolean PRODUCTION;
    private JPanel contentPane;

    // Initializing static final variables
    static {
        Yaml yaml = new Yaml();

        try (InputStream is = ChatClientGui.class.getResourceAsStream("/application.yaml")) {

            if (is == null) {
                throw new IllegalStateException("application.yaml not found");
            }

            Map<String, Object> data = yaml.load(is);

            Map<String, Object> server = (Map<String, Object>) data.get("server");
            Map<String, Object> http = (Map<String, Object>) server.get("http");
            Map<String, Object> socket = (Map<String, Object>) server.get("socket");

            HTTP_SERVER_URL = (String) http.get("url");
            SOCKET_SERVER_URL = (String) socket.get("url");
            SOCKET_SERVER_PORT = (Integer) socket.get("port");
            PRODUCTION = (Boolean) server.get("production");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application configuration", e);
        }
    }


    // Buttons
    private JButton loginButton;
    private JButton registerButton;

    // Text fields
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;

    // Labels
    private JLabel titleLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel confirmPasswordLabel;
    private JLabel toggleFormLbl;

    // Class the controls client logic, contains both an http and socket client
    private final ChatController chatController;

    public static void main(String[] args) {
        FlatDarculaLaf.setup();
        ThemeManager.setTheme("dark");

        EventQueue.invokeLater(() -> {
            try {
                new ChatClientGui();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ChatClientGui() {
        // Window title
        setTitle("ChatApp Login");

        // Basic setup
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);

        // Create the SSL HTTPS client to be passed on to ChatHttpClient class
        SSLContext sslContext = null;
        try {
            sslContext = ClientTls.create(PRODUCTION);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Create the https client
        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        // Create chat controller which contains only the http client because the socket client is only created after
        // the user authenticates
        chatController = new ChatController(
                // Create http client used for sending http requests to the http server
                new ChatHttpClient(httpClient, HTTP_SERVER_URL));

        // Checking if the user has a running session (stored refresh token)
        if(chatController.hasRefreshToken) {
            // Get the user object for the active user
            UserDTO user = chatController.getUserFromToken();

            if(user == null) {
                JOptionPane.showMessageDialog(this, "No user found for stored auth token",
                        "Auto login error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Starts upp all needed background components
            appComponentsSetup(user);

            //Open next page (main page)
            MainFrame mainFrame = new MainFrame(user, chatController);
            mainFrame.setVisible(true);
            dispose();
        } else {
            // Continue creating this frame if there is no redirect to main page automatically
            // Create components
            initializeComponents();

            // Add components to main panel
            contentPaneLayoutSetup();

            // Add panel to frame
            add(contentPane);

            // Show frame
            setVisible(true);
        }
    }

    // Creates all the necessary components
    private void initializeComponents() {
        setTitleLabel();
        setUsernameLabel();
        setPasswordLabel();
        setConfirmPasswordLabel();

        setUsernameField();
        setPasswordField();
        setConfirmPasswordField();
        setLoginButton();
        setRegisterButton();
        setToggleFormLbl();
    }

    // Adds all the components to the main panel and determines their layout
    private void contentPaneLayoutSetup() {
        // Layout setup
        contentPane = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        contentPane.add(titleLabel, gbc);

        gbc.gridwidth = 1;

        // Username label + field
        gbc.gridy++;
        contentPane.add(usernameLabel, gbc);
        gbc.gridx = 1;
        contentPane.add(usernameField, gbc);

        // Password label + field
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(passwordLabel, gbc);
        gbc.gridx = 1;
        contentPane.add(passwordField, gbc);

        // Password confirmation label + field
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(confirmPasswordLabel, gbc);
        gbc.gridx = 1;
        contentPane.add(confirmPasswordField, gbc);

        // Login button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        contentPane.add(loginButton, gbc);
        contentPane.add(registerButton, gbc);


        // Toggle form label
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        contentPane.add(toggleFormLbl, gbc);
    }

    private void validateUserInfo() throws IllegalArgumentException{
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();

        if(username.isBlank()){
            throw new IllegalArgumentException("Username is required");
        }

        if(password.length == 0) {
            throw new IllegalArgumentException("Password is required");
        }

        if(username.length() < 2) {
            throw new IllegalArgumentException("Username must be at least 2 characters long");
        }

        if(username.length() > 32) {
            throw new IllegalArgumentException("Username cannot be longer than 32 characters");
        }

        Pattern pattern = Pattern.compile("^[\\w.-]+$", Pattern.UNICODE_CHARACTER_CLASS);
        if(!pattern.matcher(username).matches()) {
            throw new IllegalArgumentException("Username can only contain alphanumerical characters and following special characters: .-_");
        }

        if(password.length < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        if(password.length > 64) {
            throw new IllegalArgumentException("Password must not be more than 64 characters long");
        }
    }

    // Clickable label to switch from register form to log in form and vice versa
    private void setToggleFormLbl() {
        toggleFormLbl = new JLabel("click here to register", SwingConstants.CENTER);
        toggleFormLbl.setForeground(Color.CYAN);
        toggleFormLbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                changeForm(toggleFormLbl.getText().equals("click here to register"));
            }
        });
    }

    // Changes form to register form if the argument is true or to login form if it is false
    private void changeForm(boolean register) {
        if(register) {
            showRegisterForm();
        } else {
            usernameField.setText("");
            passwordField.setText("");
            showLoginForm();
        }
    }

    // Displays register form fields and buttons
    private void showRegisterForm() {
        usernameField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        toggleFormLbl.setText("click here to login");
        loginButton.setVisible(false);
        registerButton.setVisible(true);
        confirmPasswordField.setVisible(true);
        confirmPasswordLabel.setVisible(true);
    }

    // Displays login form fields and buttons
    private void showLoginForm() {
        toggleFormLbl.setText("click here to register");
        loginButton.setVisible(true);
        registerButton.setVisible(false);
        confirmPasswordField.setVisible(false);
        confirmPasswordLabel.setVisible(false);

    }

    // Registration button that sends registration request on click
    private void setRegisterButton(){
        registerButton = new JButton("Register");
        registerButton.setFont(new Font("SansSerif", Font.BOLD, 22));
        registerButton.setPreferredSize(new Dimension(200, 50));
        registerButton.addActionListener(e -> {
            try{
                validateUserInfo();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Validation error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if(!Arrays.equals(passwordField.getPassword(), confirmPasswordField.getPassword())) {
                JOptionPane.showMessageDialog(this, "Passwords in the password and confirm password fields should match",
                        "Validation error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // All of this is temporary for testing purposes
            String username = usernameField.getText();
            String passwordString = new String(passwordField.getPassword());

            // Creating auth request DTO to be sent for login
            AuthRequestDTO userCredentials = AuthRequestDTO.builder()
                    .username(username)
                    .password(passwordString)
                    .build();

            // Do register here return boolean probably (popups for success/fail and on success hide register and show login button)
            int registerResponseCode = chatController.register(userCredentials);
            if(registerResponseCode == 201) {
                JOptionPane.showMessageDialog(this, "Registration successful, you may now log in!", "Success", JOptionPane.INFORMATION_MESSAGE);
                showLoginForm();
            }else if(registerResponseCode == 409) {
                JOptionPane.showMessageDialog(this, "Registration failed, user already exists!", "Registration failed", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed, try again!", "Registration error", JOptionPane.ERROR_MESSAGE);
            }
        });
        registerButton.setVisible(false);
    }

    private void setLoginButton() {
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 22));
        loginButton.setPreferredSize(new Dimension(200, 50));
        loginButton.addActionListener(e -> {
            try{
                validateUserInfo();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Validation error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // All of this is temporary for testing purposes
            String username = usernameField.getText();
            String passwordString = new String(passwordField.getPassword());

            // Creating auth request DTO to be sent for login
            AuthRequestDTO userCredentials = AuthRequestDTO.builder()
                    .username(username)
                    .password(passwordString)
                    .build();

            // Perform login request
            UserDTO user = chatController.login(userCredentials);

            // Checking if the user object is not null (user logged in successfully)
            if(user != null) {
                appComponentsSetup(user);

                //Open next page (main page)
                MainFrame mainFrame = new MainFrame(user, chatController);
                mainFrame.setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "User login failed, wrong credentials or non existing user",
                        "Login error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // Method that creates all the needed components before opening the mainframe, and injects all the dependencies for them
    private void appComponentsSetup(UserDTO user) {
        // Create the directory for saving user files if it does not exist
        LOCAL_FILES_PATH = AppPaths.getUserFilesDirectory(user.getId().toString());
        try {
            Files.createDirectories(LOCAL_FILES_PATH);
            Files.createDirectories(LOCAL_FILES_PATH.resolve("images"));
        } catch (IOException e) {
            System.out.println("Error trying to create local files directory: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error creating local files folder, closing application...",
                    "Application initialization error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }

        // Declare the Incoming Data Persistence object but initialize it in the try catch block
        IncomingDataPersistence incomingDataPersistence = null;
        // Create local database url naming it as active user id
        String dbUrl = "jdbc:sqlite:" + LOCAL_FILES_PATH.resolve("chat.db");
        try {
            // Create the database initializer and all repositories
            DatabaseManager databaseManager = new DatabaseManager(dbUrl);
            ChatRepository chatRepository = new ChatRepository(databaseManager);
            ChatMemberRepository chatMemberRepository = new ChatMemberRepository(databaseManager);
            MessageRepository messageRepository = new MessageRepository(databaseManager);
            SyncRepository syncRepository = new SyncRepository(databaseManager);

            // Initialize the IncomingDataPersistence object
            incomingDataPersistence = new IncomingDataPersistence(databaseManager, chatRepository, messageRepository);

            // Set the repositories in chat controller
            chatController.setDatabaseRepos(databaseManager,chatRepository , chatMemberRepository, messageRepository, syncRepository);
        } catch (SQLException ex) {
            System.out.println("Error creating user local database: " + ex.getMessage());
        }

        // Check if IncomingDataPersistence was set properly
        if(incomingDataPersistence == null) {
            System.out.println("Error creating incoming data persistence object.");
        }

        // Create socket client and add it to chat controller
        chatController.setSocketClient(new ChatClient(SOCKET_SERVER_URL, SOCKET_SERVER_PORT, null, incomingDataPersistence));
        // Send authMessage to authenticate the socket connection
        System.out.println("Socket connection authenticated: " + chatController.sendAuthMessage());
    }

    private void setPasswordField() {
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 20));
    }

    private void setConfirmPasswordField() {
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(new Font("SansSerif", Font.PLAIN, 20));
        confirmPasswordField.setVisible(false);
    }

    private void setUsernameField() {
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 20));
    }

    private void setPasswordLabel() {
        passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
    }

    private void setConfirmPasswordLabel() {
        confirmPasswordLabel = new JLabel("Confirm password:");
        confirmPasswordLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        confirmPasswordLabel.setVisible(false);
    }

    private void setUsernameLabel() {
        usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
    }

    private void setTitleLabel() {
        titleLabel = new JLabel("Welcome to ChatApp", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
    }

}
