package org.chatapp.server.repositories;

import org.chatapp.server.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    List<User> findAllByUsernameIsLikeIgnoreCase(String username);
    Optional<User> findByUsername(String username);
}
