package com.revendo.exception;

/**
 * Thrown when a login attempt is made on an account that has been
 * temporarily locked after too many failed attempts.
 * GlobalExceptionHandler maps this to HTTP 429 Too Many Requests.
 */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String message) {
        super(message);
    }
}
