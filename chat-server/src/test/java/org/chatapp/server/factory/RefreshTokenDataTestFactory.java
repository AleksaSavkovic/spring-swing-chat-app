package org.chatapp.server.factory;


import org.chatapp.server.entities.RefreshTokenData;
import org.chatapp.server.entities.User;

import java.time.Instant;
import java.util.UUID;

public class RefreshTokenDataTestFactory {

    public static RefreshTokenData createRefreshTokenData(User user) {
        return RefreshTokenData.builder()
                .id(UUID.randomUUID())
                .expires_at(Instant.parse("2026-06-22T12:00:00Z"))
                .user(user)
                .build();
    }

    public static RefreshTokenData createRefreshTokenData(User user, boolean revoked) {
        return RefreshTokenData.builder()
                .id(UUID.randomUUID())
                .expires_at(Instant.parse("2026-06-22T12:00:00Z"))
                .user(user)
                .revoked(revoked)
                .build();
    }
}
