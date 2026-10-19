package org.chatapp.server.integration.repository;

import jakarta.persistence.EntityManager;
import org.chatapp.server.entities.RefreshTokenData;
import org.chatapp.server.entities.User;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.repositories.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestDataHelper.class, MyTestConfiguration.class})
@ActiveProfiles("test")
public class RefreshTokenRepositoryTest {

    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    TestDataHelper testDataHelper;
    @Autowired
    EntityManager entityManager;

    @Test
    void findAllByUserId_ShouldReturn3Tokens() {
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        RefreshTokenData refreshTokenData1 = testDataHelper.saveRefreshTokenData(user1);
        RefreshTokenData refreshTokenData2 = testDataHelper.saveRefreshTokenData(user1);
        RefreshTokenData refreshTokenData3 = testDataHelper.saveRefreshTokenData(user1);
        RefreshTokenData refreshTokenData4 = testDataHelper.saveRefreshTokenData(user2);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID userId = user1.getId();

        // When
        List<RefreshTokenData> tokens = refreshTokenRepository.findAllByUserId(userId);

        // Then
        assertThat(tokens)
                .hasSize(3)
                .extracting(RefreshTokenData::getId)
                .containsExactlyInAnyOrder(
                        refreshTokenData1.getId(),
                        refreshTokenData2.getId(),
                        refreshTokenData3.getId()
                        );
    }

    @Test
    void findAllByUserId_ShouldReturnEmpty() {
        // Setup
        User user1 = testDataHelper.saveUser();
        User user2 = testDataHelper.saveUser();

        RefreshTokenData refreshTokenData1 = testDataHelper.saveRefreshTokenData(user1);
        RefreshTokenData refreshTokenData2 = testDataHelper.saveRefreshTokenData(user1);
        RefreshTokenData refreshTokenData3 = testDataHelper.saveRefreshTokenData(user1);

        entityManager.flush();
        entityManager.clear();

        // Given
        UUID userId = user2.getId();

        // When
        List<RefreshTokenData> tokens = refreshTokenRepository.findAllByUserId(userId);

        // Then
        assertThat(tokens)
                .isEmpty();
    }
}
