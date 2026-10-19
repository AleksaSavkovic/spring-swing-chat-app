package org.chatapp.server.unit.service;

import org.chatapp.common.model.AuthRequestDTO;
import org.chatapp.common.model.UserDTO;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.exceptions.UserAlreadyExistsException;
import org.chatapp.server.entities.User;
import org.chatapp.server.mappers.UserMapper;
import org.chatapp.server.repositories.UserRepository;
import org.chatapp.server.service.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void searchUsers() {
        // Given
        String query = "Joh";
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        User user1 = createTestUser(id1, "Mark Johnson");

        User user2 = createTestUser(id2, "Aljoh Sterling");

        given(userRepository.findAllByUsernameIsLikeIgnoreCase("%" + query + "%"))
                .willReturn(List.of(user1, user2));

        // Stub the Mapper
        UserDTO dto1 = UserDTO.builder().id(id1).username("Mark Johnson").build();
        UserDTO dto2 = UserDTO.builder().id(id2).username("Aljoh Sterling").build();

        given(userMapper.toDto(user1)).willReturn(dto1);
        given(userMapper.toDto(user2)).willReturn(dto2);

        // When
        List<UserDTO> users = userService.searchUsers(query);

        // Then
        assertThat(users)
                .hasSize(2)
                .extracting(UserDTO::getId, UserDTO::getUsername)
                .containsExactly(
                        tuple(id1, "Mark Johnson"),
                        tuple(id2, "Aljoh Sterling")
                );
    }

    @Test
    void searchUsersNoUserFound() {
        // Given
        String query = "Sar";
        given(userRepository.findAllByUsernameIsLikeIgnoreCase("%" + query + "%"))
                .willReturn(List.of());

        // When
        List<UserDTO> users = userService.searchUsers(query);

        // Then
        assertThat(users).hasSize(0);
    }

    @Test
    void createUser() {
        // Given
        AuthRequestDTO authRequestDTO = AuthRequestDTO.builder()
                .username("user1")
                .password("password1234")
                .build();

        UUID generatedId = UUID.randomUUID();
        String fakeHashedPassword = "hashed_password1234";

        given(passwordEncoder.encode(authRequestDTO.getPassword())).willReturn(fakeHashedPassword);
        given(userRepository.findByUsername(authRequestDTO.getUsername())).willReturn(Optional.empty());

        User mockSavedUser = User.builder()
                .id(generatedId)
                .username("user1")
                .password(fakeHashedPassword)
                .build();

        given(userRepository.save(any(User.class))).willReturn(mockSavedUser);

        UserDTO expectedDto = UserDTO.builder().id(generatedId).username("user1").build();
        given(userMapper.toDto(mockSavedUser)).willReturn(expectedDto);

        // When
        UserDTO createdUser = userService.createUser(authRequestDTO);

        // Then
        assertThat(createdUser).isNotNull()
                .extracting(UserDTO::getId, UserDTO::getUsername)
                .containsExactly(generatedId, "user1");
    }

    @Test
    void createUserAlreadyExists() {
        // Given
        AuthRequestDTO authRequestDTO = AuthRequestDTO.builder()
                .username("user1")
                .password("defaultSecurePassword123")
                .build();

        User existingUser = createTestUser(UUID.randomUUID(), "user1");

        given(userRepository.findByUsername(authRequestDTO.getUsername())).willReturn(Optional.of(existingUser));


        // Then
        // Create user service should throw an exception if a user with the same username already exists
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.createUser(authRequestDTO);
        });

        // Assures the save method was never called as the exception is thrown before it
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void hashPassword() {
        // Given
        String raw = "mySecurePassword";
        String expectedHash = "$2a$12$fakehashstring...";
        given(passwordEncoder.encode(raw)).willReturn(expectedHash);

        // When
        String actualHash = userService.hashPassword(raw);

        // Then
        assertThat(actualHash).isEqualTo(expectedHash);
        verify(passwordEncoder).encode(raw);
    }

    @Test
    void verifyPassword() {
        // Given
        String raw = "mySecurePassword";
        String hash = "$2a$12$fakehashstring...";
        given(passwordEncoder.matches(raw, hash)).willReturn(true);

        // When
        boolean isValid = userService.verifyPassword(raw, hash);

        // Then
        assertThat(isValid).isTrue();
        verify(passwordEncoder).matches(raw, hash);
    }

    @Test
    void findUsernameById() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "user1");

        // Given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // When
        String result = userService.findUsernameById(userId);

        // Then
        assertThat(user.getUsername()).isEqualTo(result);
        verify(userRepository).findById(userId);
    }

    @Test
    void findUsernameByIdUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        // Given
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // Then
        assertThrows(NotFoundException.class, () -> userService.findUsernameById(userId));
        verify(userRepository).findById(userId);
    }
    @Test
    void findUserByIdUserExists() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "user1");
        UserDTO userDto = UserDTO.builder()
                .id(userId)
                .username("user1")
                .build();

        // Given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userMapper.toDto(user)).willReturn(userDto);

        // When
        UserDTO result = userService.findUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(userId).isEqualTo(result.getId());
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }
    @Test
    void findUserByIdUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        // Given
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // Then
        assertThrows(NotFoundException.class, () -> userService.findUserById(userId));
        verify(userRepository).findById(userId);
    }


    @Test
    void getUserReference() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "user1");
        // Given
        given(userRepository.getReferenceById(userId)).willReturn(user);

        // When
        User result = userService.getUserReference(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        verify(userRepository).getReferenceById(userId);
    }

    @Test
    void getUserReferenceNotExists() {
        UUID userId = UUID.randomUUID();
        // Given
        given(userRepository.getReferenceById(userId)).willReturn(null);

        // When
        User result = userService.getUserReference(userId);

        // Then
        assertThat(result).isNull();
        verify(userRepository).getReferenceById(userId);
    }

    // Helper method for creating user
    private User createTestUser(UUID id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .password("defaultSecurePassword123")
                .createdDate(Instant.now())
                .updateDate(Instant.now())
                .build();
    }

}