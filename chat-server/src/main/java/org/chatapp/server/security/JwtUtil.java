package org.chatapp.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final Key secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refreshExpiration}")
    private Long refreshExpiration;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // Decodes the secret from base64 and returns a new secret key instance for use with HMAC-SHA algorithm
        this.secret = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );
    }

    // Method used for generating access token
    public String generateToken(CustomUserDetails userDetails){
        Map<String, Object> claims = new HashMap<>();

        return createToken(claims, userDetails.getId().toString());
    }

    // Method used for generating public token
    public String generateRefreshToken(CustomUserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getId().toString(), refreshExpiration);
    }

    // Method which calls the create token method but with expiration time added
    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, expiration);
    }

    // Method used for creating JWT token from the provided claims, subject and expiration time
    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .setId(UUID.randomUUID().toString())
                .signWith(secret, SignatureAlgorithm.HS256)
                .compact(); // Builds the actual JSON to a compact URL safe string
    }

    // Validates the token by checking if the id of the user is equal to the id in the token as well as
    // checking if the token has expired
    public Boolean validateToken(String token, CustomUserDetails userDetails) {
        final String id = extractId(token);
        return (id.equals(userDetails.getId().toString()) && !isTokenExpired(token));
    }

    // Extracts the "sub" claim from the token
    public String extractId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extracts the jti claim from the token
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    // Extracts the specified claim from the given token
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        // This line applies the function (so get subject and get date in our use cases) to the object (claims in our case)
        return claimsResolver.apply(claims);
    }

    // Extracts all claims from the given token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Check if the expiration time claim in the token is the before the current time (if the date has passed)
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Gets the Date value from the expiration claim in the token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

}
