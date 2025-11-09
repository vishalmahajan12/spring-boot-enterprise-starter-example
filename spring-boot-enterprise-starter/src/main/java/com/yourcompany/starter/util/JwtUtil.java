package com.yourcompany.starter.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * Utility class for JWT token operations.
 * 
 * Provides functionality to:
 * 1. Generate JWT tokens
 * 2. Validate JWT tokens
 * 3. Extract claims from tokens
 * 4. Check token expiration
 * 
 * Uses JJWT library for token handling.
 */
@Component
public class JwtUtil {

    /**
     * Generates a secret key from a string secret.
     */
    private SecretKey getSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extracts all claims from token.
     */
    public Claims extractAllClaims(String token, String secret) {
        return Jwts.parser()
                .verifyWith(getSigningKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts a specific claim from token.
     */
    public <T> T extractClaim(String token, String secret, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token, secret);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts username from token.
     */
    public String extractUsername(String token, String secret) {
        return extractClaim(token, secret, Claims::getSubject);
    }

    /**
     * Extracts expiration date from token.
     */
    public Date extractExpiration(String token, String secret) {
        return extractClaim(token, secret, Claims::getExpiration);
    }

    /**
     * Extracts roles from token claims.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token, String secret) {
        Claims claims = extractAllClaims(token, secret);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }
        return List.of();
    }

    /**
     * Checks if token is expired.
     */
    private Boolean isTokenExpired(String token, String secret) {
        return extractExpiration(token, secret).before(new Date());
    }

    /**
     * Validates token.
     */
    public Boolean validateToken(String token, String secret) {
        try {
            return !isTokenExpired(token, secret);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a JWT token.
     */
    public String generateToken(String username, List<String> roles, String secret, long validitySeconds) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validitySeconds * 1000);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(secret))
                .compact();
    }
}

