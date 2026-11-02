package com.revendo.security;

import com.revendo.config.JwtConfig;
import com.revendo.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Handles JWT generation, validation, and claim extraction.
 *
 * Signing algorithm: HMAC-SHA256 (HS256)
 * Claims stored:
 *   - sub  : userId (as String)
 *   - role : user role string (e.g. "ROLE_USER")
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    // ----- Key helper -----

    private SecretKey signingKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ----- Token generation -----

    /**
     * Generates a signed JWT for the given user.
     *
     * @param user the authenticated user
     * @return signed JWT string
     */
    public String generateToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    // ----- Validation -----

    /**
     * Validates a JWT string — checks signature and expiry.
     *
     * @param token the raw JWT string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("JWT unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("JWT malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.debug("JWT signature invalid: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT claims empty: {}", e.getMessage());
        }
        return false;
    }

    // ----- Claim extraction -----

    /**
     * Extracts the userId stored in the {@code sub} claim.
     *
     * @param token a validated JWT string
     * @return the user ID
     */
    public Long getUserIdFromToken(String token) {
        String subject = parseClaims(token).getSubject();
        return Long.parseLong(subject);
    }

    /**
     * Extracts the role stored in the {@code role} claim.
     *
     * @param token a validated JWT string
     * @return the role string (e.g. "ROLE_USER")
     */
    public String getRoleFromToken(String token) {
        return (String) parseClaims(token).get("role");
    }

    // ----- Internal -----

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
