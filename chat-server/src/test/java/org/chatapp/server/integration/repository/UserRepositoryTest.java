package org.chatapp.server.integration.repository;

import jakarta.persistence.EntityManager;
import org.chatapp.server.entities.User;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@DataJpaTest
@Import({TestDataHelper.class, MyTestConfiguration.class})
@ActiveProfiles("test")
public class UserRepositoryTest{

    @Autowired
    UserRepository userRepository;

    @Autowired
    TestDataHelper testDataHelper;

    @Autowired
    EntityManager entityManager;

    @Test
    void findAllByUsernameIsLikeIgnoreCase_ShouldReturn3Users() {
        // Setup
        User user1 = testDataHelper.saveUser("uSEr_kick");
        User user2 = testDataHelper.saveUser("TEST_USER_Nick");
        User user3 = testDataHelper.saveUser("Test_Mark_User");
        testDataHelper.saveUser("ron_useing");
        testDataHelper.saveUser("useful_22");
        testDataHelper.saveUser("random_guy");

        entityManager.flush();
        entityManager.clear();

        // Given
        String username = "User";

        // When
        List<User> foundUsers = userRepository.findAllByUsernameIsLikeIgnoreCase("%" + username + "%");
        foundUsers.forEach(user -> System.out.println(user.getId() + " " + user.getUsername()));
        // Then
        assertThat(foundUsers)
                .isNotEmpty()
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder(
                        user1.getUsername(),
                        user2.getUsername(),
                        user3.getUsername()
                );
    }

    @Test
    void findAllByUsernameIsLikeIgnoreCase_ShouldReturnEmptyList() {
        // Setup
        User user1 = testDataHelper.saveUser("uSEr_kick");
        User user2 = testDataHelper.saveUser("TEST_USER_Nick");
        User user3 = testDataHelper.saveUser("Test_Mark_User");
        testDataHelper.saveUser("ron_useing");
        testDataHelper.saveUser("useful_22");
        testDataHelper.saveUser("random_guy");

        entityManager.flush();
        entityManager.clear();

        // Given
        String username = "Users";

        // When
        List<User> foundUsers = userRepository.findAllByUsernameIsLikeIgnoreCase("%" + username + "%");

        // Then
        assertThat(foundUsers)
                .isEmpty();
    }

    @Test
    void findByUsername_ShouldReturnUser(){
        // Setup
        User user1 = testDataHelper.saveUser("User");
        testDataHelper.saveUser("User_Nick");
        testDataHelper.saveUser("User_Mark");
        testDataHelper.saveUser("Userr");
        testDataHelper.saveUser("Use");

        entityManager.flush();
        entityManager.clear();

        // Given
        String username = "User";

        // When
        Optional<User> foundUser = userRepository.findByUsername(username);

        // Then
        assertThat(foundUser)
                .isNotEmpty()
                .get()
                .extracting(User::getId, User::getUsername)
                .containsExactly(user1.getId(), user1.getUsername());
    }

    @Test
    void findByUsername_ShouldReturnEmptyOptional(){
        // Setup
        User user1 = testDataHelper.saveUser("USE");
        testDataHelper.saveUser("User_Nick");
        testDataHelper.saveUser("User_Mark");
        testDataHelper.saveUser("Userr");
        testDataHelper.saveUser("Use");

        entityManager.flush();
        entityManager.clear();

        // Given
        String username = "User";

        // When
        Optional<User> foundUser = userRepository.findByUsername(username);

        // Then
        assertThat(foundUser)
                .isEmpty();
    }

    @Test
    void findByUsername_ShouldThrowException(){
        // Setup
        User user1 = testDataHelper.saveUser("USEr");
        testDataHelper.saveUser("User_Nick");
        testDataHelper.saveUser("User_Mark");
        testDataHelper.saveUser("Userr");
        testDataHelper.saveUser("User");

        entityManager.flush();
        entityManager.clear();

        // Given
        String username = "User";

        // When / Then
        assertThrows(IncorrectResultSizeDataAccessException.class, () -> {
            userRepository.findByUsername(username);
        });
    }
}
