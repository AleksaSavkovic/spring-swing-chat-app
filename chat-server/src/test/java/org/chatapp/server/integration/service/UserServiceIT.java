package org.chatapp.server.integration.service;

import org.chatapp.common.model.AuthRequestDTO;
import org.chatapp.common.model.UserDTO;
import org.chatapp.server.exceptions.UserAlreadyExistsException;
import org.chatapp.server.entities.User;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import({MyTestConfiguration.class, TestDataHelper.class})
public class UserServiceIT {
    @Autowired
    UserService userService;
    @Autowired
    TestDataHelper testDataHelper;

    @Transactional
    @Test
    void searchUsers_ShouldReturn4Users() {
        // Setup
        User user1 = testDataHelper.saveUser("user12");
        User user2 = testDataHelper.saveUser("useful_guy");
        User user3 = testDataHelper.saveUser("amusement_guy");
        User user4 = testDataHelper.saveUser("peruse");
        User user5 = testDataHelper.saveUser("nothing");

        // Given
        String query = "use";

        // When
        List<UserDTO> result = userService.searchUsers(query);

        // Then
        assertThat(result)
                .isNotEmpty()
                .hasSize(4)
                .extracting(UserDTO::getId)
                .containsExactlyInAnyOrder(user1.getId(), user2.getId(), user3.getId(), user4.getId());
    }

    @Transactional
    @Test
    void searchUsers_ShouldReturnEmpty() {
        // Setup
        User user1 = testDataHelper.saveUser("user12");
        User user2 = testDataHelper.saveUser("useful_guy");
        User user3 = testDataHelper.saveUser("amusement_guy");
        User user4 = testDataHelper.saveUser("peruse");
        User user5 = testDataHelper.saveUser("nothing");

        // Given
        String query = "users";

        // When
        List<UserDTO> result = userService.searchUsers(query);

        // Then
        assertThat(result).isEmpty();
    }

    @Transactional
    @Test
    void createUser_ShouldReturnUserDTO() {
        // Setup
        User user1 = testDataHelper.saveUser("test_user1");
        User user2 = testDataHelper.saveUser("test_user2");

        // Given
        AuthRequestDTO authRequestDTO = AuthRequestDTO.builder()
                .password("random_password")
                .username("test_user")
                .build();

        // When
        UserDTO userDTO = userService.createUser(authRequestDTO);

        // Then
        assertThat(userDTO)
                .isNotNull()
                .extracting(UserDTO::getUsername)
                .isEqualTo(authRequestDTO.getUsername());

        assertThat(userDTO.getId()).isNotNull();
    }

    @Transactional
    @Test
    void createUser_ShouldThrowAlreadyExistsException() {
        // Setup
        User user1 = testDataHelper.saveUser("test_user1");
        User user2 = testDataHelper.saveUser("test_user2");

        // Given
        AuthRequestDTO authRequestDTO = AuthRequestDTO.builder()
                .password("random_password")
                .username("test_user1")
                .build();

        // When / Then
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.createUser(authRequestDTO);
        });
    }

    @Transactional
    @Test
    void findUserByUsername_ShouldReturnUserDTO() {
        // Setup
        User user = testDataHelper.saveUser("my_user");

        // Given
        String username = user.getUsername();

        // When
        Optional<UserDTO> foundUser = userService.findUserByUsername(username);

        // Then
        assertThat(foundUser)
                .isNotEmpty()
                .get()
                .extracting(UserDTO::getId, UserDTO::getUsername)
                .containsExactly(user.getId(), user.getUsername());
    }

    @Transactional
    @Test
    void findUsernameById_ShouldReturnUsername() {
        // Setup
        User user = testDataHelper.saveUser("my_user");

        // Given
        UUID userId = user.getId();

        // When
        String username = userService.findUsernameById(userId);

        // Then
        assertThat(username)
                .isNotNull()
                .isEqualTo(user.getUsername());
    }

    @Transactional
    @Test
    void findUserById_ShouldReturnUser() {
        // Setup
        User user = testDataHelper.saveUser("my_user");

        // Given
        UUID userId = user.getId();

        // When
        UserDTO foundUser = userService.findUserById(userId);

        // Then
        assertThat(foundUser)
                .isNotNull()
                .extracting(UserDTO::getId, UserDTO::getUsername)
                .containsExactly(user.getId(), user.getUsername());
    }

    @Transactional
    @Test
    void getUserReference_ShouldReturnUserEntity() {
        // Setup
        User user = testDataHelper.saveUser("my_user");

        // Given
        UUID userId = user.getId();

        // When
        User foundUser = userService.getUserReference(userId);

        // Then
        assertThat(foundUser)
                .isNotNull()
                .extracting(User::getId, User::getUsername)
                .containsExactly(user.getId(), user.getUsername());
    }
}
