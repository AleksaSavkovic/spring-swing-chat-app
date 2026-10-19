package org.chatapp.server.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chatapp.common.model.ChatDTO;
import org.chatapp.server.controller.ChatController;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.exceptions.ValidationException;
import org.chatapp.server.entities.User;
import org.chatapp.server.security.CustomUserDetails;
import org.chatapp.server.security.CustomUserDetailsService;
import org.chatapp.server.security.JwtUtil;
import org.chatapp.server.security.SecurityConfig;
import org.chatapp.server.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ChatController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean(name = "chatService")
    ChatService chatService;
    @MockitoBean
    JwtUtil jwtUtil;
    @MockitoBean
    CustomUserDetailsService userDetailsService;

    @Test
    void listUserChats_ShouldReturnChats() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String mockToken = "valid.json.token";
        List<ChatDTO> result = List.of(
                ChatDTO.builder().build(),
                ChatDTO.builder().build(),
                ChatDTO.builder().build()
        );

        authenticate(userId, mockToken);
        given(chatService.listUserChats(userId)).willReturn(result);

        // When / Then
        mockMvc.perform(get(ChatController.CHAT_PATH)
                .queryParam("userId", userId.toString())
                .header("Authorization", "Bearer " + mockToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(3)));
        verify(chatService).listUserChats(userId);
    }

    @Test
    void listUserChats_ShouldReturnForbidden() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID authenticatedUserId = UUID.randomUUID();
        String mockToken = "valid.json.token";

        authenticate(authenticatedUserId, mockToken);

        // When / Then
        mockMvc.perform(get(ChatController.CHAT_PATH)
                        .queryParam("userId", userId.toString())
                        .header("Authorization", "Bearer " + mockToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        verifyNoInteractions(chatService);
    }

    @Test
    void findChatById_ShouldReturnChat() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        String mockToken = "valid.json.token";

        authenticate(userId, mockToken);

        ChatDTO chat = ChatDTO.builder()
                .id(chatId)
                .build();

        given(chatService.isChatMember(chatId, userId)).willReturn(true);
        given(chatService.findChatById(chatId))
                .willReturn(Optional.of(chat));

        // When / Then
        mockMvc.perform(get(ChatController.CHAT_PATH_ID, chatId)
                        .header("Authorization", "Bearer " + mockToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(chatId.toString()));

        verify(chatService).findChatById(chatId);
    }

    @Test
    void findChatById_ShouldReturnNotFound() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        String mockToken = "valid.json.token";

        authenticate(userId, mockToken);

        given(chatService.isChatMember(chatId, userId)).willThrow(NotFoundException.class);
        given(chatService.findChatById(chatId))
                .willReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get(ChatController.CHAT_PATH_ID, chatId)
                        .queryParam("userId", userId.toString())
                        .header("Authorization", "Bearer " + mockToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void findChatById_ShouldReturnForbidden() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID authenticatedUserId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        String mockToken = "valid.json.token";

        authenticate(authenticatedUserId, mockToken);

        given(chatService.isChatMember(chatId, userId)).willReturn(false);

        // When / Then
        mockMvc.perform(get(ChatController.CHAT_PATH_ID, chatId)
                        .queryParam("userId", authenticatedUserId.toString())
                        .header("Authorization", "Bearer " + mockToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createChat_ShouldReturnCreated() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        String mockToken = "valid.json.token";
        ChatDTO chat = ChatDTO.builder().build();

        authenticate(userId, mockToken);

        ChatDTO saved = ChatDTO.builder()
                .id(chatId)
                .build();

        given(chatService.saveChat(any(ChatDTO.class)))
                .willReturn(saved);

        // When / Then
        mockMvc.perform(post(ChatController.CHAT_PATH)
                        .header("Authorization", "Bearer " + mockToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chat)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        ChatController.CHAT_PATH + "/" + chatId));

        verify(chatService).saveChat(any(ChatDTO.class));
    }

    @Test
    void createChat_ShouldReturnBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        String mockToken = "valid.json.token";

        authenticate(userId, mockToken);

        mockMvc.perform(post(ChatController.CHAT_PATH)
                        .header("Authorization", "Bearer " + mockToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString("null")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatService);
    }

    @Test
    void createChat_ShouldReturnBadRequestValidationError() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String mockToken = "valid.json.token";
        ChatDTO chat = ChatDTO.builder().build();

        authenticate(userId, mockToken);

        given(chatService.saveChat(any(ChatDTO.class)))
                .willThrow(new ValidationException("A chat must contain at least two users."));

        // When / Then
        mockMvc.perform(post(ChatController.CHAT_PATH)
                        .header("Authorization", "Bearer " + mockToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chat)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A chat must contain at least two users."))
                .andExpect(jsonPath("$.status").value(400))
                .andReturn();

        verify(chatService).saveChat(any(ChatDTO.class));
    }

    private void authenticate(UUID userId, String token) {
        CustomUserDetails user = new CustomUserDetails(
                User.builder()
                        .id(userId)
                        .username("test-user")
                        .password("password")
                        .build());

        given(jwtUtil.extractId(token)).willReturn(userId.toString());
        given(userDetailsService.loadUserById(userId.toString())).willReturn(user);
        given(jwtUtil.validateToken(token, user)).willReturn(true);
    }
}
