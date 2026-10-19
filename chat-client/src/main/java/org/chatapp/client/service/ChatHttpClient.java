package org.chatapp.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chatapp.common.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class ChatHttpClient {
    private final HttpClient httpClient;
    private final String serverUrl;
    private final ObjectMapper objectMapper;
    private final String apiUrl;

    public ChatHttpClient(HttpClient httpClient, String serverUrl) {
        this.httpClient = httpClient;
        this.serverUrl = serverUrl;
        this.apiUrl = serverUrl + "/api/v1";
        this.objectMapper = new ObjectMapper();
    }

    // Gets all chats for the active user from the ChatController in the API
    public List<ChatDTO> getChats(UUID activeUser, String token) {
        HttpResponse<String> response = sendGetRequest(apiUrl + "/chat?userId=" + activeUser, token);

        List<ChatDTO> chatDTOS;
        try {
            chatDTOS = objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            System.out.println("Error reading chats from get chats http method: " + e.getMessage());
            return null;
        }

        return chatDTOS;
    }

    // Gets a page containing a set number of messages from the MessageController in the API
    // Think about a way to store page for each chat so that you load the correct messages when scrolling up
    public List<MessageDTO> getChatMessages(UUID chatId, int pageNumber, int pageSize, String token) {
        HttpResponse<String> response = sendGetRequest(apiUrl + "/message?chatId=" + chatId
                + "&pageNumber=" + pageNumber + "&pageSize=" + pageSize, token);

        PageResponse<MessageDTO> messagePage;
        try {
            messagePage = objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Error reading messages from get chat messages http method",e);
        }

        // Take out the message list from page object and reverse it (it is sorted from newest to oldest in the backend)
        List<MessageDTO> messageDTOList = messagePage.getContent();
        Collections.reverse(messageDTOList);

        return messageDTOList;
    }

    // This method sends a get request to the sync endpoint and receives a sync response dto with new chats and messages
    public SyncResponseDTO sync(UUID userId, Instant lastSync, String token) {
        String requestString;
        // Checking if lastSync is null, based on that we build the request with appropriate request parameters
        if(lastSync == null) {
            requestString = apiUrl + "/sync?userId=" + userId;
        } else {
            requestString = apiUrl + "/sync?userId=" + userId + "&lastSync=" + lastSync;
        }
        HttpResponse<String> response = sendGetRequest(requestString, token);

        SyncResponseDTO syncResponse;
        try {
            syncResponse = objectMapper.readValue(response.body(), SyncResponseDTO.class);
        } catch (JsonProcessingException e) {
            System.out.println("Error reading sync response from sync http method: " + e.getMessage());
            return null;
        }

        return syncResponse;
    }


    // This method sends a get request to the sync/status endpoint and receives a list of updated message statuses
    public List<MessageStatusChangeDTO> syncStatuses(UUID chatId, Instant firstMsg, Instant lastSync, String token) {
        String requestString;
        // Checking if lastSync is null, based on that we build the request with appropriate request parameters
        if(lastSync == null) {
            requestString = apiUrl + "/sync/statuses?chatId=" + chatId + "&firstMsg=" + firstMsg;
        } else {
            requestString = apiUrl + "/sync/statuses?chatId=" + chatId + "&firstMsg=" + firstMsg + "&lastSync=" + lastSync;
        }
        HttpResponse<String> response = sendGetRequest(requestString, token);

        List<MessageStatusChangeDTO> messageStatuses;
        try {
            messageStatuses = objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            System.out.println("Error reading message statuses from syncStatuses http method: " + e.getMessage());
            return null;
        }

        return messageStatuses;
    }

    // Post request for creating a new chat
    public HttpResponse<String> createChat(ChatDTO chat, String token) {
        String requestBody;
        // Convert ChatDTO object to json string to be used as the request body
        try {
            requestBody = objectMapper.writeValueAsString(chat);
        } catch (JsonProcessingException e) {
            System.out.println("Error creating create chat request for createChat http method: " + e.getMessage());
            return null;
        }

        return sendPostRequest("/chat", requestBody, token);
    }

    // Get request that finds a chat by url containing chat UUID
    public ChatDTO findChat(String locationUrl, String token) {
        HttpResponse<String> response = sendGetRequest(serverUrl + locationUrl, token);

        ChatDTO foundChat;
        try {
            foundChat = objectMapper.readValue(response.body(), ChatDTO.class);
        } catch (IOException e) {
            System.out.println("Error reading found chat from findChat http method: " + e.getMessage());
            return null;
        }

        return foundChat;
    }

    // Find user by user id
    public UserDTO findUserById(UUID userId, String token) {
        HttpResponse<String> response = sendGetRequest(apiUrl + "/user/" + userId, token);

        UserDTO foundUser;
        try {
            foundUser = objectMapper.readValue(response.body(), UserDTO.class);
        } catch (IOException e) {
            System.out.println("Error reading found user from findUser http method: " + e.getMessage());
            return null;
        } catch (NullPointerException e) {
            System.out.println("User with id " + userId + " not found");
            return null;
        }

        return foundUser;
    }

    // Find message by message id
    public MessageDTO findMessageById(UUID messageId, String token) {
        HttpResponse<String> response = sendGetRequest(apiUrl + "/message/" + messageId, token);

        MessageDTO foundMessage;
        try{
            foundMessage = objectMapper.readValue(response.body(), MessageDTO.class);
        } catch (IOException e) {
            System.out.println("Error reading found user from findUser http method: " + e.getMessage());
            return null;
        } catch (NullPointerException e) {
            System.out.println("Message with id " + messageId + " not found");
            return null;
        }
        return foundMessage;
    }

    // Return a set of messageDTOs for the set of ids sent to server
    public List<MessageDTO> findMessagesWhereIdIn(Set<UUID> messageIds, String token) {
        String requestBody;
        // Convert messageIds set to json string to be used as the request body
        try {
            requestBody = objectMapper.writeValueAsString(messageIds);
        } catch (JsonProcessingException e) {
            System.out.println("Error creating request body for getMessagesByIds http method: " + e.getMessage());
            return null;
        }

        // Send POST request and convert the response body to a list of MessageDTOs
        HttpResponse<String> response = sendPostRequest("/message/by-ids", requestBody, token);
        List<MessageDTO> messageDTOs;
        try {
            if(response == null) {
                return null;
            }
            messageDTOs = objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            System.out.println("Error reading messages from getMessagesByIds http method: " + e.getMessage());
            return null;
        }
        return messageDTOs;
    }

    // Post request for login which returns the TokenResponseDTO containing our accessToken and refreshToken
    public TokenResponseDTO login(AuthRequestDTO userInfo) {
       String requestBody;
       try{
            requestBody = objectMapper.writeValueAsString(userInfo);
       } catch (JsonProcessingException e) {
           throw new RuntimeException(e);
       }
       HttpResponse<String> response = sendPostRequest("/auth/login", requestBody, null);

       TokenResponseDTO tokens = null;
       if(response.statusCode() == 200) {
           try {
               tokens = objectMapper.readValue(response.body(), TokenResponseDTO.class);
           } catch (JsonProcessingException e) {
               System.out.println("Error reading tokenResponseDTO from login http method: " + e.getMessage());
               return null;
           }
       }
       return tokens;
    }


    // Post request for registration http response containing location of newly created user in the 'Location' header
    public HttpResponse<String> register(AuthRequestDTO userInfo) {
        String requestBody;
        try{
            requestBody = objectMapper.writeValueAsString(userInfo);
        } catch (JsonProcessingException e) {
            System.out.println("Error creating request body for register http method: " + e.getMessage());
            return null;
        }
        return sendPostRequest("/auth/register", requestBody, null);
    }


    // Post request used to send refresh request to get a new access token by using the refresh token
    public TokenResponseDTO sendRefresh(String refreshToken) {
        String requestBody = "{\"refreshToken\": \"" + refreshToken +"\"}";
        HttpResponse<String> response = sendPostRequest("/auth/refresh", requestBody, null);

        TokenResponseDTO tokens = null;
        if(response.statusCode() == 200) {
            try {
                tokens = objectMapper.readValue(response.body(), TokenResponseDTO.class);
            } catch (JsonProcessingException e) {
                System.out.println("Error reading TokenResponseDTO from sendRefresh http method: " + e.getMessage());
                return null;
            }
        }
        return tokens;
    }

    // Get request for logout that returns a response entity with status code
    public HttpResponse<String> logout(String token) {
        return sendPostRequest("/auth/logout", null, token);
    }

    // Post request for adding a user to the chat room
    //    public boolean addUserToRoom() {
    //        return true;
    //    }

        // Post request for leaving a chat room
    //    public UUID leaveChatRoom() {
    //        return leftChatRoom;
    //    }

    // Gets the file attachment linked in a message
    public HttpResponse<byte[]> getMessageAttachment(UUID messageId, String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        apiUrl + "/message/" + messageId + "/image"
                ))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        System.out.println("Sending get request for message attachment: " + request);
        try {
            HttpResponse<byte[]> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() != 200) {
                System.out.println("Image download failed: HTTP " + response.statusCode());
                return null;
            }

            return response;
        }
        catch (IOException e) {
            System.out.println("Error sending multipart file get request: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted while waiting for multipart file get response: " + e.getMessage());
            return null;
        }
    }

    // Sends post request sending image file as multipart file to the server
    public HttpResponse<?> sendImageFilePostRequest(Path image, String token){
        MultipartBodyPublisher multipart =
                null;
        try {
            multipart = new MultipartBodyPublisher()
                    .addFile(
                            "file",
                            image,
                            "image/jpeg"
                    );
        } catch (IOException e) {
            System.out.println("Error creating multipart file.");
            return null;
        }

        // Create the post request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/message/image"))
                .header("Content-Type", multipart.contentType())
                .header("Authorization", "Bearer " + token)
                .POST(multipart.build())
                .build();
        HttpResponse<String> response;
        try {
            response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );
        } catch (IOException e) {
            // Add better exception handling, show gui pop up on error
            System.out.println("Error sending multipart file post request: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted while waiting for multipart file post response: " + e.getMessage());
            return null;
        }
        return response;
    }

    // Sends post request with the given json body to the specified endpoint
    private HttpResponse<String> sendPostRequest(String endpoint, String body, String token) {
        String url = serverUrl + "/api/v1" + endpoint;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5));

        if(body != null) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
        }else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }

        // Adding authorization header with the token if needed
        if(token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.build();

        System.out.println(request);

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            // Add better exception handling, show gui pop up on error
            System.out.println("Error sending post request: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted while waiting for post response: " + e.getMessage());
            return null;
        }
        return response;
    }

    // Sends get request to the given url
    private HttpResponse<String> sendGetRequest(String url, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5)) // Optional: set a timeout
                .header("Content-Type", "application/json"); // Optional: add headers

        // Adding authorization with the token header if needed
        if(token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.build();

        System.out.println(request);

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            System.out.println("Error sending get request: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted while waiting for get response: " + e.getMessage());
            return null;
        }
        return response;
    }


}
