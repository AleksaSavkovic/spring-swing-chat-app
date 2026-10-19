package org.chatapp.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.common.model.AuthRequestDTO;
import org.chatapp.common.model.TokenResponseDTO;
import org.chatapp.common.model.UserDTO;
import org.chatapp.server.entities.RefreshTokenData;
import org.chatapp.server.security.CustomUserDetails;
import org.chatapp.server.security.CustomUserDetailsService;
import org.chatapp.server.security.JwtUtil;
import org.chatapp.server.service.RefreshTokenService;
import org.chatapp.server.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AuthController {

    public static final String AUTH_PATH = "/api/v1/auth";

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    // Save user with encoded password
    @PostMapping(AUTH_PATH + "/register")
    public ResponseEntity register(@RequestBody AuthRequestDTO authInfo) {
        UserDTO newUser = userService.createUser(authInfo);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/api/v1/user/" + newUser.getId().toString());

        return new ResponseEntity(headers, HttpStatus.CREATED);
    }

    @PostMapping(AUTH_PATH + "/login")
    public ResponseEntity createAuthenticationToken (@RequestBody AuthRequestDTO authRequest) {
        // Authentication the user with user credentials(username and password)
        try{
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );
        } catch(BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect username or password");
        }

        // Get the user details for the user that is authenticating
        final CustomUserDetails userDetails = (CustomUserDetails) userDetailsService
                .loadUserByUsername(authRequest.getUsername());

        // Making sure that any old refresh tokens for the user are no longer usable as login generates a new one
        refreshTokenService.revokeUserTokens(userDetails.getId());

        final String jwt = jwtUtil.generateToken(userDetails);
        final String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // Create the refresh token entity so it can be saved to the database
        RefreshTokenData refreshEntity = RefreshTokenData.builder()
                .id(UUID.fromString(jwtUtil.extractJti(refreshToken)))
                .user(userService.getUserReference(userDetails.getId()))
                .revoked(false)
                .expires_at(jwtUtil.extractExpiration(refreshToken).toInstant())
                .build();

        // Saving the refresh token entity to the database
        refreshTokenService.saveRefreshToken(refreshEntity);

        return ResponseEntity.ok(TokenResponseDTO.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .build());
    }

    @PostMapping(AUTH_PATH + "/refresh")
    public ResponseEntity refreshAuthenticationToken (@RequestBody Map<String, String> refreshRequest) {
        String refreshToken = refreshRequest.get("refreshToken");

        // Checks if refresh token is valid and not null
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken,
                // Extract user id from "sub" claim of the token and use it to find the user in userDetailsService
                (CustomUserDetails) userDetailsService.loadUserById(jwtUtil.extractId(refreshToken)))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract "jti" claim from the token
        UUID jti = UUID.fromString(jwtUtil.extractJti(refreshToken));

        // Check if the refresh token was revoked and if it was stop the user from refreshing the access token
        if (refreshTokenService.checkIfTokenIsRevoked(jti)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get id from the token and load user details object with it from userDetailsService
        final String userId = jwtUtil.extractId(refreshToken);
        final CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserById(userId);
        final String newJwt = jwtUtil.generateToken(userDetails); // Generate a new JWT with the user details

        return ResponseEntity.ok(TokenResponseDTO.builder()
                .accessToken(newJwt)
                .refreshToken(refreshToken)
                .build());
    }

    @PostMapping(AUTH_PATH + "/logout")
    public ResponseEntity logout(@RequestHeader("Authorization") String bearerToken) {
        // Not checking if the format is correct because this is a protected endpoint
        String token = bearerToken.substring(7);

        // Revokes user's refresh tokens so that he cannot get any new access tokens without logging in again
        refreshTokenService.revokeUserTokens(UUID.fromString(jwtUtil.extractId(token)));
        return ResponseEntity.noContent().build();
    }

}
