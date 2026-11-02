package com.revendo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception-to-HTTP-response mapping for the entire API.
 *
 * Standard error body:
 * <pre>
 * {
 *   "status":    400,
 *   "error":     "Bad Request",
 *   "message":   "Human-readable description.",
 *   "timestamp": "2024-01-15T10:30:00Z"
 * }
 * </pre>
 *
 * For Bean Validation failures (400), an extra "errors" map is included:
 * <pre>
 * {
 *   "status":  400,
 *   "error":   "Bad Request",
 *   "message": "Validation failed",
 *   "errors":  { "email": "must be a well-formed email address", ... },
 *   "timestamp": "..."
 * }
 * </pre>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ----------------------------------------------------------------
    // 404 — Not Found
    // ----------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 401 — Unauthorized
    // ----------------------------------------------------------------

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 403 — Forbidden  (Spring AccessDeniedException + suspended account)
    // ----------------------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler(SuspendedAccountException.class)
    public ResponseEntity<Map<String, Object>> handleSuspended(SuspendedAccountException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 400 — Bad Request (business validation)
    // ----------------------------------------------------------------

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 400 — Bad Request (Bean Validation via @Valid)
    // ----------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        // keep first message when multiple violations exist for the same field
                        (first, second) -> first
                ));

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Validation failed");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ----------------------------------------------------------------
    // 409 — Conflict
    // ----------------------------------------------------------------

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 429 — Too Many Requests (account lockout)
    // ----------------------------------------------------------------

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleLockout(AccountLockedException ex) {
        return error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 500 — Internal Server Error (catch-all)
    // ----------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(buildBody(status, message));
    }

    private Map<String, Object> buildBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
