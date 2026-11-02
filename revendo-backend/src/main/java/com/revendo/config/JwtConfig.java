package com.revendo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JwtConfig holds the JWT secret key and expiry duration.
 *
 * Values are read from application.properties:
 *   app.jwt.secret         - the HMAC-SHA256 signing secret
 *   app.jwt.expiration-ms  - token lifetime in milliseconds (86400000 = 24 hours)
 *
 * In production, set the JWT_SECRET environment variable.
 * Never commit real secrets to version control.
 */
@Component
public class JwtConfig {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public String getSecret() {
        return secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}