package org.chatapp.server.repositories;


import org.chatapp.server.entities.RefreshTokenData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenData, UUID> {
        List<RefreshTokenData> findAllByUserId(UUID userId);
}
