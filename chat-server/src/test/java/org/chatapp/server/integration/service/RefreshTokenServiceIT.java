package org.chatapp.server.integration.service;

import org.chatapp.server.exceptions.TokenRefreshException;
import org.chatapp.server.entities.RefreshTokenData;
import org.chatapp.server.entities.User;
import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import({MyTestConfiguration.class, TestDataHelper.class})
public class RefreshTokenServiceIT {
    @Autowired
    RefreshTokenService refreshTokenService;
    @Autowired
    TestDataHelper testDataHelper;

    @Transactional
    @Test
    void revokeUserTokens_RevokesAll() {
        // Setup
        User user = testDataHelper.saveUser();
        User otherUser = testDataHelper.saveUser();
        RefreshTokenData refreshTokenData = testDataHelper.saveRefreshTokenData(user);
        RefreshTokenData refreshTokenData1 = testDataHelper.saveRefreshTokenData(user);
        RefreshTokenData refreshTokenData2 = testDataHelper.saveRefreshTokenData(user, true);
        RefreshTokenData refreshTokenData3 = testDataHelper.saveRefreshTokenData(otherUser, true);

        // Given
        UUID userId = user.getId();

        // When
        List<RefreshTokenData> revokedTokens = refreshTokenService.revokeUserTokens(userId);

        // Then
        assertThat(revokedTokens)
                .isNotEmpty()
                .extracting(RefreshTokenData::getId, RefreshTokenData::isRevoked)
                .containsExactlyInAnyOrder(
                        tuple(refreshTokenData.getId(), true),
                        tuple(refreshTokenData1.getId(), true),
                        tuple(refreshTokenData2.getId(), true)
                );
    }

    @Transactional
    @Test
    void saveRefreshToken_SavesToken() {
        // Setup
        User user = testDataHelper.saveUser();

        // Given
        RefreshTokenData refreshTokenData = RefreshTokenData.builder()
                .id(UUID.randomUUID())
                .user(user)
                .revoked(false)
                .expires_at(Instant.now().plusSeconds(1000))
                .build();

        // When
        RefreshTokenData savedToken = refreshTokenService.saveRefreshToken(refreshTokenData);

        // Then
        assertThat(savedToken)
                .isNotNull()
                .extracting(
                        RefreshTokenData:: getId,
                        refreshTokenData1 -> refreshTokenData1.getUser().getId(),
                        RefreshTokenData::isRevoked,
                        RefreshTokenData::getExpires_at)
                .containsExactlyInAnyOrder(
                        refreshTokenData.getId(),
                        refreshTokenData.getUser().getId(),
                        refreshTokenData.isRevoked(),
                        refreshTokenData.getExpires_at()
                );
    }

    @Transactional
    @Test
    void checkIfTokenIsRevoked_ShouldReturnIsRevoked(){
        // Setup
        User user = testDataHelper.saveUser();
        User otherUser = testDataHelper.saveUser();
        RefreshTokenData refreshTokenData = testDataHelper.saveRefreshTokenData(user);
        RefreshTokenData refreshTokenData1 = testDataHelper.saveRefreshTokenData(user);
        RefreshTokenData refreshTokenData2 = testDataHelper.saveRefreshTokenData(user, true);
        RefreshTokenData refreshTokenData3 = testDataHelper.saveRefreshTokenData(otherUser, true);

        // Given
        UUID tokenId = refreshTokenData2.getId();

        // When
        boolean isRevoked = refreshTokenService.checkIfTokenIsRevoked(tokenId);

        // Then
        assertThat(isRevoked).isTrue();
    }

    @Transactional
    @Test
    void checkIfTokenIsRevoked_ShouldReturnIsNotRevoked(){
        // Setup
        User user = testDataHelper.saveUser();
        User otherUser = testDataHelper.saveUser();
        RefreshTokenData refreshTokenData = testDataHelper.saveRefreshTokenData(user);
        RefreshTokenData refreshTokenData1 = testDataHelper.saveRefreshTokenData(user);
        RefreshTokenData refreshTokenData2 = testDataHelper.saveRefreshTokenData(user, true);
        RefreshTokenData refreshTokenData3 = testDataHelper.saveRefreshTokenData(otherUser, true);

        // Given
        UUID tokenId = refreshTokenData1.getId();

        // When
        boolean isRevoked = refreshTokenService.checkIfTokenIsRevoked(tokenId);

        // Then
        assertThat(isRevoked).isFalse();
    }

    @Transactional
    @Test
    void checkIfTokenIsRevoked_ShouldThrowIllegalArgumentException(){
        // Setup
        User user = testDataHelper.saveUser();
        User otherUser = testDataHelper.saveUser();
        RefreshTokenData refreshTokenData = testDataHelper.saveRefreshTokenData(user);
        RefreshTokenData refreshTokenData1 = testDataHelper.saveRefreshTokenData(user);
        RefreshTokenData refreshTokenData2 = testDataHelper.saveRefreshTokenData(user, true);
        RefreshTokenData refreshTokenData3 = testDataHelper.saveRefreshTokenData(otherUser, true);

        // Given
        UUID tokenId = UUID.randomUUID();

        // When / Then
        assertThrows(TokenRefreshException.class, () -> {
            refreshTokenService.checkIfTokenIsRevoked(tokenId);
        });
    }
}
