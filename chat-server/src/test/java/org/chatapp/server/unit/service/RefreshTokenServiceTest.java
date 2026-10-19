package org.chatapp.server.unit.service;

import org.chatapp.server.exceptions.TokenRefreshException;
import org.chatapp.server.entities.RefreshTokenData;
import org.chatapp.server.repositories.RefreshTokenRepository;
import org.chatapp.server.service.RefreshTokenServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    RefreshTokenServiceImpl refreshTokenService;

    @Test
    void testRevokeUserTokens_ShouldReturnRevokedTokens() {
        // Given
        UUID userId = UUID.randomUUID();

        given(refreshTokenRepository.findAllByUserId(userId)).willReturn(List.of(
                RefreshTokenData.builder().revoked(false).build(),
                RefreshTokenData.builder().revoked(false).build(),
                RefreshTokenData.builder().revoked(false).build()
        ));

        // When
        List<RefreshTokenData> result = refreshTokenService.revokeUserTokens(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result)
                .hasSize(3)
                .extracting(RefreshTokenData::isRevoked)
                .containsExactly(true, true, true);
    }

    @Test
    void checkIfTokenIsRevoked_ShouldReturnTrue(){
        // Given
        UUID tokenId = UUID.randomUUID();
        RefreshTokenData refreshTokenData = RefreshTokenData.builder().id(tokenId).revoked(true).build();

        given(refreshTokenRepository.findById(tokenId)).willReturn(Optional.of(refreshTokenData));

        // When
        boolean result = refreshTokenService.checkIfTokenIsRevoked(tokenId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void checkIfTokenIsRevoked_ShouldReturnFalse(){
        // Given
        UUID tokenId = UUID.randomUUID();
        RefreshTokenData refreshTokenData = RefreshTokenData.builder().id(tokenId).revoked(false).build();

        given(refreshTokenRepository.findById(tokenId)).willReturn(Optional.of(refreshTokenData));

        // When
        boolean result = refreshTokenService.checkIfTokenIsRevoked(tokenId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void checkIfTokenIsRevoked_ShouldThrowException(){
        // Given
        UUID tokenId = UUID.randomUUID();

        given(refreshTokenRepository.findById(tokenId)).willReturn(Optional.empty());

        // When / Then
        assertThrows(TokenRefreshException.class, () -> {
            refreshTokenService.checkIfTokenIsRevoked(tokenId);
        });
    }

}
