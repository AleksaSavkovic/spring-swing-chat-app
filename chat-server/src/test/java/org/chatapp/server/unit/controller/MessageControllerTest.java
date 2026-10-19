package org.chatapp.server.unit.controller;

import org.chatapp.common.model.MessageDTO;
import org.chatapp.server.controller.MessageController;
import org.chatapp.server.exceptions.ResourceForbiddenException;
import org.chatapp.server.entities.User;
import org.chatapp.server.security.CustomUserDetails;
import org.chatapp.server.security.CustomUserDetailsService;
import org.chatapp.server.security.JwtUtil;
import org.chatapp.server.security.SecurityConfig;
import org.chatapp.server.service.ImageStorageService;
import org.chatapp.server.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class MessageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MessageService messageService;
    @MockitoBean
    ImageStorageService imageStorageService;

    @MockitoBean
    JwtUtil jwtUtil;
    @MockitoBean
    CustomUserDetailsService userDetailsService;

    @Test
    void listChatMessages_ShouldReturnPage() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        String mockToken = "valid.json.token";

        authenticate(userId, mockToken);

        PageImpl<MessageDTO> mockPage = new PageImpl<>(Collections.emptyList());
        given(messageService.getChatMessages(eq(chatId), any(), any(), eq(userId)))
                .willReturn(mockPage);

        // When / Then
        mockMvc.perform(get(MessageController.MESSAGE_PATH)
                        .queryParam("chatId", chatId.toString())
                        .queryParam("pageNumber", "0")
                        .queryParam("pageSize", "10")
                        .header("Authorization", "Bearer " + mockToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

    }

    @Test
    void listChatMessages_ShouldReturnForbidden() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        String mockToken = "valid.json.token";

        authenticate(userId, mockToken);

        PageImpl<MessageDTO> mockPage = new PageImpl<>(Collections.emptyList());
        given(messageService.getChatMessages(eq(chatId), any(), any(), eq(userId)))
                .willThrow(ResourceForbiddenException.class);

        // When / Then
        mockMvc.perform(get(MessageController.MESSAGE_PATH)
                        .queryParam("chatId", chatId.toString())
                        .queryParam("pageNumber", "0")
                        .queryParam("pageSize", "10")
                        .header("Authorization", "Bearer " + mockToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

    }

    @Test
    void listChatMessages_ShouldReturnBadRequest() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String mockToken = "valid.json.token";

        authenticate(userId, mockToken);

        // When / Then
        mockMvc.perform(get(MessageController.MESSAGE_PATH)
                        .queryParam("pageNumber", "0")
                        .queryParam("pageSize", "10")
                        .header("Authorization", "Bearer " + mockToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

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
