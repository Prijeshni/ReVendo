package com.revendo.service;

// Feature: re-vendo-marketplace
// Property 1: Registration validation rejects any invalid input
// Property 2: Registered user round-trip — password is never stored in plaintext
// Property 4: Login lockout after repeated failures

import com.revendo.config.JwtConfig;
import com.revendo.dto.request.LoginRequest;
import com.revendo.dto.request.RegisterRequest;
import com.revendo.exception.AccountLockedException;
import com.revendo.exception.ConflictException;
import com.revendo.exception.ValidationException;
import com.revendo.model.User;
import com.revendo.repository.UserRepository;
import com.revendo.security.JwtTokenProvider;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NumericChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for {@link AuthService}.
 *
 * All tests are pure unit tests — no Spring context, no database.
 * The repository is stubbed via {@link StubUserRepository}.
 *
 * Properties covered:
 *   Property 1 — Validates: Requirements 2.3, 2.4, 2.5, 2.6, 2.7
 *   Property 2 — Validates: Requirements 2.8, 14.6
 *   Property 4 — Validates: Requirements 3.13
 */
class AuthServicePropertyTest {

    private AuthService     authService;
    private PasswordEncoder passwordEncoder;

    /**
     * jqwik lifecycle: runs before each @Property method.
     * Wires a real BCrypt encoder + a stub repository + real JwtTokenProvider.
     */
    @BeforeProperty
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtTokenProvider jwtProvider = buildJwtProvider();
        // Default stub: no existing user — email not taken
        authService = new AuthService(
                new StubUserRepository(), passwordEncoder, jwtProvider);
    }

    // ================================================================
    // Property 1 — Registration validation rejects any invalid input
    // Validates: Requirements 2.3, 2.4, 2.5, 2.6, 2.7
    // ================================================================

    /**
     * Property 1a: Any email without the '@' character must be rejected.
     */
    @Property(tries = 100)
    void property1a_invalidEmailWithoutAtSymbol(
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String localPart,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String domain
    ) {
        // Concatenate two alpha strings with no '@' in between
        String badEmail = localPart + domain;
        // Guard: if by chance it contains '@', skip this sample
        Assume.that(!badEmail.contains("@"));

        RegisterRequest req = validBase(badEmail, "12345678", "ValidPass1");

        assertThatThrownBy(() -> authService.register(req))
                .as("Email without '@' must throw ValidationException or ConflictException")
                .isInstanceOfAny(ValidationException.class, ConflictException.class);
    }

    /**
     * Property 1b: Password shorter than 8 characters must be rejected.
     */
    @Property(tries = 100)
    void property1b_passwordTooShort(
            @ForAll @AlphaChars @StringLength(min = 1, max = 7) String shortPassword
    ) {
        RegisterRequest req = validBase("jane@example.com", "12345678", shortPassword);

        assertThatThrownBy(() -> authService.register(req))
                .as("Password < 8 chars must throw ValidationException")
                .isInstanceOf(ValidationException.class);
    }

    /**
     * Property 1c: Password longer than 128 characters must be rejected.
     */
    @Property(tries = 50)
    void property1c_passwordTooLong(
            @ForAll @AlphaChars @StringLength(min = 129, max = 200) String longPassword
    ) {
        RegisterRequest req = validBase("jane@example.com", "12345678", longPassword);

        assertThatThrownBy(() -> authService.register(req))
                .as("Password > 128 chars must throw ValidationException")
                .isInstanceOf(ValidationException.class);
    }

    /**
     * Property 1d: Phone numbers with fewer than 8 digits must be rejected.
     */
    @Property(tries = 100)
    void property1d_phoneTooShort(
            @ForAll @NumericChars @StringLength(min = 1, max = 7) String shortPhone
    ) {
        RegisterRequest req = validBase("jane@example.com", shortPhone, "ValidPass1");

        assertThatThrownBy(() -> authService.register(req))
                .as("Phone < 8 digits must throw ValidationException")
                .isInstanceOf(ValidationException.class);
    }

    /**
     * Property 1e: Phone numbers with more than 15 digits must be rejected.
     */
    @Property(tries = 50)
    void property1e_phoneTooLong(
            @ForAll @NumericChars @StringLength(min = 16, max = 25) String longPhone
    ) {
        RegisterRequest req = validBase("jane@example.com", longPhone, "ValidPass1");

        assertThatThrownBy(() -> authService.register(req))
                .as("Phone > 15 digits must throw ValidationException")
                .isInstanceOf(ValidationException.class);
    }

    /**
     * Property 1f: Mismatched password / confirmPassword must be rejected.
     */
    @Property(tries = 100)
    void property1f_passwordConfirmMismatch(
            @ForAll @AlphaChars @StringLength(min = 8, max = 20) String password,
            @ForAll @AlphaChars @StringLength(min = 8, max = 20) String confirm
    ) {
        Assume.that(!password.equals(confirm));

        RegisterRequest req = RegisterRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .phoneNumber("12345678")
                .password(password)
                .confirmPassword(confirm)
                .build();

        assertThatThrownBy(() -> authService.register(req))
                .as("Mismatched passwords must throw ValidationException")
                .isInstanceOf(ValidationException.class);
    }

    // ================================================================
    // Property 2 — Password never stored in plaintext
    // Validates: Requirements 2.8, 14.6
    // ================================================================

    /**
     * Property 2: For any valid plaintext password, the stored hash must differ
     * from the plaintext AND must verify correctly with BCrypt.
     */
    @Property(tries = 50)
    void property2_passwordNeverStoredInPlaintext(
            @ForAll @AlphaChars @StringLength(min = 8, max = 40) String plainPassword
    ) {
        AtomicReference<User> savedUser = new AtomicReference<>();

        // Capturing stub: records the User that gets passed to save()
        UserRepository capturingStub = new StubUserRepository() {
            @Override
            public <S extends User> S save(S entity) {
                savedUser.set(entity);
                return entity;
            }
        };

        AuthService svc = new AuthService(
                capturingStub, passwordEncoder, buildJwtProvider());

        RegisterRequest req = RegisterRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .phoneNumber("12345678")
                .password(plainPassword)
                .confirmPassword(plainPassword)
                .build();

        svc.register(req);

        User stored = savedUser.get();
        assertThat(stored).isNotNull();

        // Assert 1: hash is NOT equal to the plaintext string
        assertThat(stored.getPasswordHash())
                .as("Password hash must differ from plaintext")
                .isNotEqualTo(plainPassword);

        // Assert 2: BCrypt can verify the stored hash against the plaintext
        assertThat(passwordEncoder.matches(plainPassword, stored.getPasswordHash()))
                .as("BCrypt must verify plaintext against the stored hash")
                .isTrue();
    }

    // ================================================================
    // Property 4 — Login lockout after 5 consecutive failures
    // Validates: Requirements 3.13
    // ================================================================

    /**
     * Property 4: After 5 failed login attempts, the account is locked.
     * Any subsequent attempt — even with the correct password — must be
     * rejected with {@link AccountLockedException} within the lockout window.
     */
    @Property(tries = 30)
    void property4_lockoutAfterFiveFailedAttempts(
            @ForAll @AlphaChars @StringLength(min = 8, max = 20) String correctPassword
    ) {
        String email = "locktest@example.com";
        String hash  = passwordEncoder.encode(correctPassword);

        AtomicReference<User> userRef = new AtomicReference<>(
                User.builder()
                        .id(1L)
                        .email(email)
                        .passwordHash(hash)
                        .firstName("Lock")
                        .lastName("Test")
                        .phoneNumber("12345678")
                        .role(User.Role.ROLE_USER)
                        .status(User.Status.Active)
                        .failedLoginAttempts(0)
                        .build()
        );

        // Mutable stub: lookups and saves operate on the in-memory userRef
        UserRepository mutableStub = new StubUserRepository() {
            @Override
            public Optional<User> findByEmail(String searchEmail) {
                return email.equals(searchEmail)
                        ? Optional.of(userRef.get())
                        : Optional.empty();
            }
            @Override
            public <S extends User> S save(S entity) {
                userRef.set(entity);
                return entity;
            }
        };

        AuthService svc = new AuthService(
                mutableStub, passwordEncoder, buildJwtProvider());

        String wrongPassword = correctPassword + "_WRONG";

        // Simulate 5 consecutive failed attempts
        for (int i = 0; i < 5; i++) {
            try {
                svc.login(LoginRequest.builder()
                        .email(email)
                        .password(wrongPassword)
                        .build());
            } catch (ValidationException ignored) {
                // expected "Invalid credentials."
            }
        }

        // Account must now be locked
        assertThat(userRef.get().getLockedUntil())
                .as("lockedUntil must be set after 5 failed attempts")
                .isNotNull();
        assertThat(userRef.get().getFailedLoginAttempts())
                .as("failedLoginAttempts must be >= 5")
                .isGreaterThanOrEqualTo(5);

        // Even a correct-password attempt must now throw AccountLockedException
        assertThatThrownBy(() ->
                svc.login(LoginRequest.builder()
                        .email(email)
                        .password(correctPassword)
                        .build()))
                .as("Correct password must still be rejected within the lockout window")
                .isInstanceOf(AccountLockedException.class);
    }

    // ================================================================
    // Helpers
    // ================================================================

    private RegisterRequest validBase(String email, String phone, String password) {
        return RegisterRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email(email)
                .phoneNumber(phone)
                .password(password)
                .confirmPassword(password)
                .build();
    }

    private JwtTokenProvider buildJwtProvider() {
        JwtConfig config = new JwtConfig();
        injectField(config, "secret", "testSecretForAuthServiceTests32chars!!");
        injectField(config, "expirationMs", 86_400_000L);
        return new JwtTokenProvider(config);
    }

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
