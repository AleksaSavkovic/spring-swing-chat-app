package org.chatapp.server.service;

import org.chatapp.server.entities.RefreshTokenData;

import java.util.List;
import java.util.UUID;

public interface RefreshTokenService {
    List<RefreshTokenData> revokeUserTokens(UUID userId);
    RefreshTokenData saveRefreshToken(RefreshTokenData refreshTokenData);
    boolean checkIfTokenIsRevoked(UUID tokenId);
}
