package com.revendo.security;

// Feature: re-vendo-marketplace, Property 3: JWT issuance and validation round-trip

import com.revendo.config.JwtConfig;
import com.revendo.model.User;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link JwtTokenProvider}.
 *
 * Property 3: JWT issuance and validation round-trip
 * Validates: Requirements 3.2, 3.5
 *
 * For any arbitrary (userId, role) pair:
 *   1. A generated token must pass validateToken().
 *   2. getUserIdFromToken() must equal the original userId.
 *   3. getRoleFromToken() must equal the original role string.
 */
class JwtTokenProviderPropertyTest {

    private JwtTokenProvider jwtTokenProvider;

    /**
     * jqwik lifecycle hook — runs before each @Property method.
     * Use @BeforeProperty (not JUnit's @BeforeEach) so jqwik calls it.
     */
    @BeforeProperty
    void setUp() {
        JwtConfig config = new JwtConfig();
        injectField(config, "secret", "testSecretKeyForPropertyTestsMustBe32BytesLong!!");
        injectField(config, "expirationMs", 86_400_000L);
        jwtTokenProvider = new JwtTokenProvider(config);
    }

    // ----- Arbitraries -----

    /** Generates positive Long user IDs (1 … Long.MAX_VALUE). */
    @Provide
    Arbitrary<Long> positiveUserIds() {
        return Arbitraries.longs().greaterOrEqual(1L);
    }

    /** Standard role strings used in the application. */
    @Provide
    Arbitrary<String> roleStrings() {
        return Arbitraries.of("ROLE_USER", "ROLE_ADMIN");
    }

    // ----- Properties -----

    /**
     * Property 3: JWT round-trip — userId and role are preserved exactly.
     */
    @Property(tries = 100)
    void jwtRoundTrip(
            @ForAll("positiveUserIds") Long userId,
            @ForAll("roleStrings")    String role
    ) {
        // Build a minimal User stub with the generated id and role
        User user = User.builder()
                .id(userId)
                .role(User.Role.valueOf(role))
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("12345678")
                .passwordHash("$2a$10$hash")
                .build();

        // Act
        String token = jwtTokenProvider.generateToken(user);

        // Assert 1: token passes signature and expiry validation
        assertThat(jwtTokenProvider.validateToken(token))
                .as("validateToken must return true for a freshly issued token")
                .isTrue();

        // Assert 2: userId claim survives the round-trip
        assertThat(jwtTokenProvider.getUserIdFromToken(token))
                .as("getUserIdFromToken must return the original userId")
                .isEqualTo(userId);

        // Assert 3: role claim survives the round-trip
        assertThat(jwtTokenProvider.getRoleFromToken(token))
                .as("getRoleFromToken must return the original role")
                .isEqualTo(role);
    }

    /**
     * Property: an obviously invalid token string always fails validation.
     */
    @Property(tries = 50)
    void invalidTokensAreRejected(
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String garbage
    ) {
        assertThat(jwtTokenProvider.validateToken(garbage))
                .as("validateToken must return false for garbage input")
                .isFalse();
    }

    // ----- Reflection helper -----

    /** Injects a private field value without requiring a public setter. */
    private static void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject field: " + fieldName, e);
        }
    }
}
