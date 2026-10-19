package org.chatapp.server.unit.controller;

import org.chatapp.common.model.MessageStatusChangeDTO;
import org.chatapp.common.model.SyncResponseDTO;
import org.chatapp.server.controller.SyncController;
import org.chatapp.server.entities.User;
import org.chatapp.server.security.CustomUserDetails;
import org.chatapp.server.security.CustomUserDetailsService;
import org.chatapp.server.security.JwtUtil;
import org.chatapp.server.security.SecurityConfig;
import org.chatapp.server.service.ChatService;
import org.chatapp.server.service.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@WebMvcTest(SyncController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class SyncControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtUtil jwtUtil;
    @MockitoBean
    CustomUserDetailsService userDetailsService;
    @MockitoBean
    SyncService syncService;
    @MockitoBean(name = "chatService")
    ChatService chatService;

    @Test
    void syncUserData_ShouldReturnData() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String mockToken = "valid.json.token";
        Instant fixedNow = Instant.parse("2026-06-24T12:00:00Z");
        Instant lastSync = Instant.parse("2026-06-22T12:00:00Z");

        SyncResponseDTO response = SyncResponseDTO.builder()
                .serverTime(fixedNow)
                .build();

        authenticate(userId, mockToken);

        given(syncService.syncUserData(userId, lastSync))
                .willReturn(response);

        // When / Then
        mockMvc.perform(get(SyncController.SYNC_PATH)
                        .queryParam("userId", String.valueOf(userId))
                        .queryParam("lastSync", String.valueOf(lastSync))
                        .header("Authorization", "Bearer " + mockToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.serverTime").value(String.valueOf(fixedNow)));

        verify(syncService).syncUserData(userId, lastSync);
    }

    @Test
    void syncUserData_ShouldReturnForbidden() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID activeUser = UUID.randomUUID();
        String mockToken = "valid.json.token";

        authenticate(activeUser, mockToken);

        // When / Then
        mockMvc.perform(get(SyncController.SYNC_PATH)
                        .queryParam("userId", String.valueOf(userId))
                        .queryParam("lastSync", Instant.now().toString())
                        .header("Authorization", "Bearer " + mockToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(syncService);
    }

    @Test
    void syncUserData_shouldReturnBadRequest() throws Exception {
        // Given
        UUID activeUser = UUID.randomUUID();
        String mockToken = "valid.json.token";

        authenticate(activeUser, mockToken);

        // When / Then
        mockMvc.perform(get(SyncController.SYNC_PATH)
                        .queryParam("lastSync", Instant.now().toString())
                        .header("Authorization", "Bearer " + mockToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(syncService);
    }

    @Test
    void syncChatMessageStatuses_shouldReturnStatuses() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        String mockToken = "valid.json.token";
        Instant firstMsg = Instant.parse("2026-06-20T12:00:00Z");
        Instant lastSync = Instant.parse("2026-06-22T12:00:00Z");

        List<MessageStatusChangeDTO> response = List.of(
                MessageStatusChangeDTO
                        .builder()
                        .id(UUID.randomUUID())
                        .build(),
                MessageStatusChangeDTO
                        .builder()
                        .id(UUID.randomUUID())
                        .build()
        );

        authenticate(userId, mockToken);

        given(chatService.isChatMember(chatId, userId)).willReturn(true);

        given(syncService.syncChatMsgStatuses(chatId, firstMsg, lastSync))
                .willReturn(response);

        // When / Then
        mockMvc.perform(get(SyncController.SYNC_PATH + "/statuses")
                        .queryParam("chatId", String.valueOf(chatId))
                        .queryParam("firstMsg", String.valueOf(firstMsg))
                        .queryParam("lastSync", String.valueOf(lastSync))
                        .header("Authorization", "Bearer " + mockToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2));

        verify(syncService).syncChatMsgStatuses(chatId, firstMsg, lastSync);
    }

    @Test
    void syncChatMessageStatuses_shouldReturnForbidden() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        String mockToken = "valid.json.token";
        Instant firstMsg = Instant.parse("2026-06-20T12:00:00Z");
        Instant lastSync = Instant.parse("2026-06-22T12:00:00Z");

        authenticate(userId, mockToken);

        given(chatService.isChatMember(chatId, userId)).willReturn(false);

        // When / Then
        mockMvc.perform(get(SyncController.SYNC_PATH + "/statuses")
                        .queryParam("chatId", String.valueOf(chatId))
                        .queryParam("firstMsg", String.valueOf(firstMsg))
                        .queryParam("lastSync", String.valueOf(lastSync))
                        .header("Authorization", "Bearer " + mockToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(syncService);
    }

    @Test
    void syncChatMessageStatuses_shouldReturnBadRequest() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        String mockToken = "valid.json.token";
        Instant lastSync = Instant.parse("2026-06-22T12:00:00Z");

        authenticate(userId, mockToken);

        given(chatService.isChatMember(chatId, userId)).willReturn(true);

        // When / Then
        mockMvc.perform(get(SyncController.SYNC_PATH + "/statuses")
                        .queryParam("chatId", String.valueOf(chatId))
                        .queryParam("lastSync", String.valueOf(lastSync))
                        .header("Authorization", "Bearer " + mockToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(syncService);
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
