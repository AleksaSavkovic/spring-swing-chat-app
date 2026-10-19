package org.chatapp.server.unit.controller;

import org.chatapp.common.model.UserDTO;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.controller.UserController;
import org.chatapp.server.entities.User;
import org.chatapp.server.security.CustomUserDetails;
import org.chatapp.server.security.CustomUserDetailsService;
import org.chatapp.server.security.JwtUtil;
import org.chatapp.server.security.SecurityConfig;
import org.chatapp.server.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserService userService;

    @Test
    void findUserById_ShouldReturnUser() throws Exception {
        // Given
        UserDTO testUser = UserDTO.builder().id(UUID.randomUUID()).username("test_user").build();
        String mockToken = "valid.json.token";

        CustomUserDetails mockUserDetails = new CustomUserDetails(User.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .password("password").build());

        given(jwtUtil.extractId(mockToken)).willReturn(String.valueOf(testUser.getId()));
        given(userDetailsService.loadUserById(String.valueOf(testUser.getId()))).willReturn(mockUserDetails);
        given(jwtUtil.validateToken(mockToken, mockUserDetails)).willReturn(true);

        given(userService.findUserById(testUser.getId())).willReturn(testUser);

        // When / Then
        mockMvc.perform(get(UserController.USER_PATH_ID, testUser.getId())
                .header("Authorization", "Bearer " + mockToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()));
    }

    @Test
    void findUserById_ShouldReturnNotFound() throws Exception {
        // Given
        UserDTO testUser = UserDTO.builder().id(UUID.randomUUID()).username("test_user").build();
        String mockToken = "valid.json.token";

        CustomUserDetails mockUserDetails = new CustomUserDetails(User.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .password("password").build());

        given(jwtUtil.extractId(mockToken)).willReturn(String.valueOf(testUser.getId()));
        given(userDetailsService.loadUserById(String.valueOf(testUser.getId()))).willReturn(mockUserDetails);
        given(jwtUtil.validateToken(mockToken, mockUserDetails)).willReturn(true);

        given(userService.findUserById(testUser.getId())).willThrow(NotFoundException.class);

        // When / Then
        mockMvc.perform(get(UserController.USER_PATH_ID, testUser.getId())
                        .header("Authorization", "Bearer " + mockToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void findUserById_ShouldReturnForbidden() throws Exception {
        // Given
        UserDTO testUser = UserDTO.builder().id(UUID.randomUUID()).username("test_user").build();
        String mockToken = "valid.json.token";

        given(userService.findUserById(testUser.getId())).willReturn(testUser);
        // When / Then
        mockMvc.perform(get(UserController.USER_PATH_ID, testUser.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void findUserById_ShouldReturnForbiddenWhenTokenIsInvalid() throws Exception {
        // Given
        UUID testUserId = UUID.randomUUID();
        String badToken = "invalid.json.token";

        CustomUserDetails mockUserDetails = new CustomUserDetails(User.builder()
                .id(testUserId)
                .username("test_user").build());

        given(jwtUtil.extractId(badToken)).willReturn(String.valueOf(testUserId));
        given(userDetailsService.loadUserById(String.valueOf(testUserId))).willReturn(mockUserDetails);
        given(jwtUtil.validateToken(badToken, mockUserDetails)).willReturn(false);

        // When / Then
        mockMvc.perform(get(UserController.USER_PATH_ID, testUserId)
                        .header("Authorization", "Bearer " + badToken))
                .andExpect(status().isForbidden());
    }


}
