package org.chatapp.server.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chatapp.common.model.AuthRequestDTO;
import org.chatapp.common.model.UserDTO;
import org.chatapp.server.controller.AuthController;
import org.chatapp.server.exceptions.UserAlreadyExistsException;
import org.chatapp.server.controller.UserController;
import org.chatapp.server.entities.RefreshTokenData;
import org.chatapp.server.entities.User;
import org.chatapp.server.security.CustomUserDetails;
import org.chatapp.server.security.CustomUserDetailsService;
import org.chatapp.server.security.JwtUtil;
import org.chatapp.server.security.SecurityConfig;
import org.chatapp.server.service.RefreshTokenService;
import org.chatapp.server.service.UserService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthenticationManager authenticationManager;
    @MockitoBean
    UserService userService;
    @MockitoBean
    RefreshTokenService refreshTokenService;
    @MockitoBean
    JwtUtil jwtUtil;
    @MockitoBean
    CustomUserDetailsService userDetailsService;

    @Test
    void register_ShouldReturnCreated() throws Exception {
        // Given
        AuthRequestDTO authInfo = AuthRequestDTO.builder()
                .username("new-user")
                .password("secure-password")
                .build();

        UserDTO createdUser = UserDTO.builder()
                .username("new-user")
                .id(UUID.randomUUID())
                .build();

        given(userService.createUser(authInfo)).willReturn(createdUser);

        // When / Then
        mockMvc.perform(post(AuthController.AUTH_PATH + "/register")
                        .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authInfo)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", UserController.USER_PATH + "/" + createdUser.getId()));
        verify(userService).createUser(authInfo);
    }

    @Test
    void register_ShouldReturnConflict() throws Exception {
        // Given
        AuthRequestDTO authInfo = AuthRequestDTO.builder()
                .username("new-user")
                .password("secure-password")
                .build();

        given(userService.createUser(authInfo)).willThrow(UserAlreadyExistsException.class);

        // When / Then
        mockMvc.perform(post(AuthController.AUTH_PATH + "/register")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authInfo)))
                .andExpect(status().isConflict()).andReturn();
        verify(userService).createUser(authInfo);
    }

    @Test
    void createAuthenticationToken_ShouldReturnTokenResponse() throws Exception {
        // Given
        AuthRequestDTO authInfo = AuthRequestDTO.builder()
                .username("new-user")
                .password("secure-password")
                .build();
        CustomUserDetails foundUser = new CustomUserDetails(User.builder()
                .username("new-user")
                .password("secure-password")
                .id(UUID.randomUUID())
                .build());
        String accessToken = "valid.access.token";
        String refreshToken = "valid.refresh.token";
        UUID jti = UUID.randomUUID();
        Date expiration = Date.from(Instant.now().plusSeconds(3600));
        Authentication authentication = mock(Authentication.class);


        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authentication);
        given(userDetailsService.loadUserByUsername(authInfo.getUsername())).willReturn(foundUser);
        given(refreshTokenService.revokeUserTokens(foundUser.getId())).willReturn(List.of());
        given(jwtUtil.generateToken(foundUser)).willReturn(accessToken);
        given(jwtUtil.generateRefreshToken(foundUser)).willReturn(refreshToken);
        given(jwtUtil.extractJti(refreshToken)).willReturn(jti.toString());
        given(userService.getUserReference(foundUser.getId())).willReturn(User.builder().id(foundUser.getId()).build());
        given(jwtUtil.extractExpiration(refreshToken)).willReturn(expiration);
        given(refreshTokenService.saveRefreshToken(any())).willReturn(RefreshTokenData.builder().build());

        // When / Then
        mockMvc.perform(post(AuthController.AUTH_PATH + "/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(accessToken))
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenService).revokeUserTokens(foundUser.getId());
        verify(jwtUtil).generateToken(foundUser);
        verify(jwtUtil).generateRefreshToken(foundUser);
    }

    @Test
    void createAuthenticationToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        AuthRequestDTO authInfo = AuthRequestDTO.builder()
                .username("new-user")
                .password("secure-password")
                .build();

        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("bad"));

        // When / Then
        mockMvc.perform(post(AuthController.AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authInfo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshAuthenticationToken_ShouldReturnRefreshAndNewAccessToken() throws Exception {
        // Given
        String refreshToken = "valid.refresh.token";
        String newToken = "valid.access.token";
        UUID jti = UUID.randomUUID();
        CustomUserDetails foundUser = new CustomUserDetails(User.builder()
                .username("new-user")
                .password("secure-password")
                .id(UUID.randomUUID())
                .build());

        given(jwtUtil.validateToken(refreshToken, foundUser)).willReturn(true);
        given(jwtUtil.extractJti(refreshToken)).willReturn(jti.toString());
        given(refreshTokenService.checkIfTokenIsRevoked(jti)).willReturn(false);
        given(jwtUtil.extractId(refreshToken)).willReturn(foundUser.getId().toString());
        given(userDetailsService.loadUserById(foundUser.getId().toString())).willReturn(foundUser);
        given(jwtUtil.generateToken(foundUser)).willReturn(newToken);

        mockMvc.perform(post(AuthController.AUTH_PATH + "/refresh")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newToken))
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));

        verify(jwtUtil).validateToken(refreshToken, foundUser);
        verify(jwtUtil).extractJti(refreshToken);
        verify(jwtUtil, times(2)).extractId(refreshToken);
        verify(jwtUtil).generateToken(foundUser);
        verify(refreshTokenService).checkIfTokenIsRevoked(jti);
        verify(userDetailsService, times(2)).loadUserById(foundUser.getId().toString());
    }

    @Test
    void refreshAuthenticationToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        String refreshToken = "valid.refresh.token";
        String jti = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();
        CustomUserDetails foundUser = new CustomUserDetails(User.builder().build());

        given(jwtUtil.validateToken(refreshToken, foundUser)).willReturn(false);
        given(jwtUtil.extractId(refreshToken)).willReturn(userId.toString());
        given(userDetailsService.loadUserById(userId.toString())).willReturn(foundUser);

        // When / Then
        mockMvc.perform(post(AuthController.AUTH_PATH + "/refresh")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());

        verify(jwtUtil).validateToken(refreshToken, foundUser);
        verify(jwtUtil).extractId(refreshToken);
        verify(jwtUtil, never()).generateToken(foundUser);
        verify(jwtUtil, never()).extractJti(jti);
        verify(refreshTokenService, never()).checkIfTokenIsRevoked(UUID.fromString(jti));
        verify(userDetailsService).loadUserById(userId.toString());
    }

    @Test
    void refreshAuthenticationToken_ShouldReturnForbidden() throws Exception {
        // Given
        String refreshToken = "valid.refresh.token";
        String jti = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        given(jwtUtil.extractId(refreshToken)).willReturn(userId.toString());
        given(userDetailsService.loadUserById(userId.toString())).willThrow(UsernameNotFoundException.class);

        // When / Then
        mockMvc.perform(post(AuthController.AUTH_PATH + "/refresh")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isForbidden());

        verify(jwtUtil).extractId(refreshToken);
        verify(jwtUtil, never()).generateToken(any());
        verify(jwtUtil, never()).extractJti(jti);
        verify(refreshTokenService, never()).checkIfTokenIsRevoked(UUID.fromString(jti));
        verify(userDetailsService).loadUserById(userId.toString());
    }

    @Test
    void logout_ShouldReturnNoContent() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String mockToken = "valid.json.token";
        authenticate(userId, mockToken);

        given(refreshTokenService.revokeUserTokens(userId)).willReturn(List.of(RefreshTokenData.builder().build()));

        // When / Then
        mockMvc.perform(post(AuthController.AUTH_PATH + "/logout")
                .header("Authorization", "Bearer " + mockToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(refreshTokenService).revokeUserTokens(userId);
    }

    @Test
    void logout_ShouldReturnForbidden() throws Exception {
        // Given
        String mockToken = "valid.json.token";

        // When / Then
        mockMvc.perform(post(AuthController.AUTH_PATH + "/logout")
                        .header("Authorization", "Bearer " + mockToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        verifyNoInteractions(refreshTokenService);
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
