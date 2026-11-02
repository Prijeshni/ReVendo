package com.revendo.service;

import com.revendo.dto.request.LoginRequest;
import com.revendo.dto.request.RegisterRequest;
import com.revendo.dto.response.AuthResponse;
import com.revendo.exception.AccountLockedException;
import com.revendo.exception.ConflictException;
import com.revendo.exception.SuspendedAccountException;
import com.revendo.exception.ValidationException;
import com.revendo.model.User;
import com.revendo.repository.UserRepository;
import com.revendo.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Authentication service — handles user registration, login, and lockout logic.
 *
 * Requirements covered: 2.2–2.10, 3.2–3.5, 3.13, 14.8
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    // Lock window after 5 failed attempts
    private static final int    MAX_FAILED_ATTEMPTS  = 5;
    private static final int    LOCKOUT_MINUTES      = 15;

    // Phone: 8–15 digits only
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\d{8,15}$");

    // Basic email regex (RFC-5322 light; javax @Email handles full validation in DTO)
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // ----------------------------------------------------------------
    // Registration  (Requirement 2.x)
    // ----------------------------------------------------------------

    /**
     * Registers a new user account.
     *
     * Validations performed (in order):
     * 1. Email format
     * 2. Email uniqueness
     * 3. Phone number: 8–15 digits
     * 4. Password length: 8–128 characters
     * 5. Password confirmation match  (front-end also validates, but checked here too)
     *
     * @param dto the incoming registration payload
     * @return a success message
     * @throws ValidationException if any business rule is violated
     * @throws ConflictException   if the email is already registered
     */
    @Transactional
    public String register(RegisterRequest dto) {

        // 1. Email format
        if (!EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
            throw new ValidationException("Email address format is invalid.");
        }

        // 2. Email uniqueness
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("Email address is already in use.");
        }

        // 3. Phone number: 8–15 digits
        if (!PHONE_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
            throw new ValidationException(
                    "Phone number must contain between 8 and 15 digits.");
        }

        // 4. Password length: 8–128 characters
        String password = dto.getPassword();
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new ValidationException(
                    "Password must be between 8 and 128 characters.");
        }

        // 5. Password confirmation
        if (!password.equals(dto.getConfirmPassword())) {
            throw new ValidationException(
                    "Password and confirm password do not match.");
        }

        // Build and save user
        User user = User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(password))
                .role(User.Role.ROLE_USER)
                .status(User.Status.Active)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(user);

        return "Account created successfully. Please log in.";
    }

    // ----------------------------------------------------------------
    // Login + lockout  (Requirement 3.x)
    // ----------------------------------------------------------------

    /**
     * Authenticates a user and returns a JWT.
     *
     * Flow:
     * 1. Find user by email — return generic error if not found.
     * 2. Check if account is Suspended → 403.
     * 3. Check if account is locked (lockedUntil in future) → 429.
     * 4. Verify password:
     *    - On failure: increment failedLoginAttempts; lock if >= 5.
     *    - On success: reset failedLoginAttempts, clear lockedUntil, issue JWT.
     *
     * @param dto the login payload
     * @return an {@link AuthResponse} containing the JWT and user info
     */
    @Transactional
    public AuthResponse login(LoginRequest dto) {

        // 1. Look up user — use generic message to avoid enumeration
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ValidationException("Invalid credentials."));

        // 2. Suspended account
        if (user.getStatus() == User.Status.Suspended) {
            throw new SuspendedAccountException("This account has been suspended.");
        }

        // 3. Locked account
        if (user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccountLockedException(
                    "Account temporarily locked. Try again in 15 minutes.");
        }

        // 4. Password check
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            handleFailedAttempt(user);
            throw new ValidationException("Invalid credentials.");
        }

        // Success — reset counters
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token       = jwtTokenProvider.generateToken(user);
        String displayName = user.getFirstName() + " " + user.getLastName();

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .userId(user.getId())
                .displayName(displayName)
                .build();
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    /**
     * Increments failed login counter; locks the account if threshold is reached.
     */
    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
        }

        userRepository.save(user);
    }
}
