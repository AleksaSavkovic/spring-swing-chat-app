package org.chatapp.server.service;

import lombok.RequiredArgsConstructor;
import org.chatapp.server.exceptions.TokenRefreshException;
import org.chatapp.server.entities.RefreshTokenData;
import org.chatapp.server.repositories.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // On login revoke any existing refresh tokens for the user
    @Transactional
    @Override
    public List<RefreshTokenData> revokeUserTokens(UUID userId) {
        List<RefreshTokenData> refreshTokens = refreshTokenRepository.findAllByUserId(userId);

        refreshTokens.forEach(refreshTokenData -> refreshTokenData.setRevoked(true));

        return refreshTokens;
    }

    // When a refresh token is generated save it to the database
    @Override
    public RefreshTokenData saveRefreshToken(RefreshTokenData refreshTokenData) {
        return refreshTokenRepository.save(refreshTokenData);
    }


    // When refreshing access token check if refresh token is revoked
    @Override
    public boolean checkIfTokenIsRevoked(UUID tokenId) {
        RefreshTokenData token = refreshTokenRepository.findById(tokenId).orElseThrow(TokenRefreshException::new);
        return token.isRevoked();
    }
}
