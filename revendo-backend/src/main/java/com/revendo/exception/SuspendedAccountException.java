package com.revendo.exception;

/**
 * Thrown when a login attempt is made on a suspended account.
 * GlobalExceptionHandler maps this to HTTP 403 Forbidden.
 */
public class SuspendedAccountException extends RuntimeException {

    public SuspendedAccountException(String message) {
        super(message);
    }
}
