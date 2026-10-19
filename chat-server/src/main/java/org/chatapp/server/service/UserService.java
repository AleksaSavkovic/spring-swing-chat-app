package org.chatapp.server.service;

import org.chatapp.common.model.AuthRequestDTO;
import org.chatapp.common.model.UserDTO;
import org.chatapp.server.entities.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    List<UserDTO> searchUsers(String query);

    String hashPassword(String rawPassword);

    boolean verifyPassword(String rawPassword, String encodedPassword);

    Optional<UserDTO> findUserByUsername(String username);

    UserDTO createUser(AuthRequestDTO authRequest);

    String findUsernameById(UUID id);

    UserDTO findUserById(UUID id);

    User getUserReference(UUID id);
}
