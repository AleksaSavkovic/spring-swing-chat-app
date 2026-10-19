package org.chatapp.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.common.model.AuthRequestDTO;
import org.chatapp.common.model.UserDTO;
import org.chatapp.server.exceptions.NotFoundException;
import org.chatapp.server.exceptions.UserAlreadyExistsException;
import org.chatapp.server.entities.User;
import org.chatapp.server.mappers.UserMapper;
import org.chatapp.server.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // Searches the database for users with username similar to the search query
    @Override
    public List<UserDTO> searchUsers(String query) {
        return userRepository.findAllByUsernameIsLikeIgnoreCase("%" + query + "%")
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    // Creates the user with credentials obtained from registration form
    @Override
    public UserDTO createUser(AuthRequestDTO authRequest) {
        Optional<User> found = userRepository.findByUsername(authRequest.getUsername());

        if(found.isPresent()) {
            throw new UserAlreadyExistsException();
        }

        User user = User.builder()
                .username(authRequest.getUsername())
                .password(hashPassword(authRequest.getPassword()))
                .build();

        User saved = userRepository.save(user);

        return userMapper.toDto(saved);
    }

    // Hash password using our bcrypt password encoder
    @Override
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }


    // Verify password using bcrypt password encoder
    @Override
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public Optional<UserDTO> findUserByUsername(String username) {
        return Optional.ofNullable(userMapper.toDto(userRepository.findByUsername(username)
                .orElse(null)));
    }

    @Override
    public String findUsernameById(UUID id) {
        User foundUser = userRepository.findById(id).orElseThrow(NotFoundException::new);
        return foundUser.getUsername();
    }

    @Override
    public UserDTO findUserById(UUID userId) {
        return userMapper.toDto(userRepository.findById(userId).orElseThrow(NotFoundException::new));
    }

    @Override
    public User getUserReference(UUID id) {
        return userRepository.getReferenceById(id);
    }
}
