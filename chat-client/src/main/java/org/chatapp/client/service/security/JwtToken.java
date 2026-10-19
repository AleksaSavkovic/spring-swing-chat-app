package org.chatapp.client.service.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class JwtToken {

    private final String tokenEncoded;
    private final UUID userId;
    private final Instant expiryDate;

    public JwtToken(String token) {
        tokenEncoded = token;

        // Splitting the token by . and taking out the payload (the middle of 3 blocks)
        String[] chunks = tokenEncoded.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(chunks[1]));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = null;
        try {
            node = mapper.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String sub = node.get("sub").asText();
        long exp = node.get("exp").asLong();

        if (sub == null || exp == 0) {
            throw new RuntimeException("Invalid information in JWT token");
        }

        userId = UUID.fromString(sub);
        expiryDate = Instant.ofEpochSecond(exp);
    }

    public String getTokenEncoded() {
        return tokenEncoded;
    }

    public UUID getTokenUser() {
        return userId;
    }

    public boolean isExpired() {
        return expiryDate
                .minusSeconds(60)
                .isBefore(Instant.now());
    }

}
