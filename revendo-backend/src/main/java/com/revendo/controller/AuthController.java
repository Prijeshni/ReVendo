package com.revendo.controller;

import com.revendo.dto.request.LoginRequest;
import com.revendo.dto.request.RegisterRequest;
import com.revendo.dto.response.AuthResponse;
import com.revendo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authentication endpoints.
 *
 * Public routes (no JWT required — see SecurityConfig):
 *   POST /api/auth/register  →  201 Created
 *   POST /api/auth/login     →  200 OK
 *
 * Error responses are handled globally by {@link com.revendo.exception.GlobalExceptionHandler}.
 *
 * Requirements: 2.1, 3.1, 14.7
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ----------------------------------------------------------------
    // POST /api/auth/register
    // ----------------------------------------------------------------

    /**
     * Register a new user account.
     *
     * <pre>
     * Request body:
     * {
     *   "firstName":       "Jane",
     *   "lastName":        "Doe",
     *   "email":           "jane@example.com",
     *   "phoneNumber":     "12345678",
     *   "password":        "securePass1",
     *   "confirmPassword": "securePass1"
     * }
     *
     * Response 201:
     * { "message": "Account created successfully. Please log in." }
     *
     * Response 400:  field-level validation failure (Bean Validation)
     * Response 400:  business-rule violation (phone, password, confirm mismatch)
     * Response 409:  email already in use
     * </pre>
     *
     * @param request validated registration payload
     * @return 201 with success message
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request) {

        String message = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", message));
    }

    // ----------------------------------------------------------------
    // POST /api/auth/login
    // ----------------------------------------------------------------

    /**
     * Authenticate an existing user and receive a signed JWT.
     *
     * <pre>
     * Request body:
     * { "email": "jane@example.com", "password": "securePass1" }
     *
     * Response 200:
     * { "token": "<JWT>", "role": "ROLE_USER", "userId": 1, "displayName": "Jane Doe" }
     *
     * Response 400:  missing/blank fields or invalid credentials
     * Response 403:  account suspended
     * Response 429:  account temporarily locked
     * </pre>
     *
     * @param request validated login payload
     * @return 200 with JWT and user metadata
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
