package com.revendo.exception;

/**
 * Thrown when a request lacks valid authentication credentials.
 * GlobalExceptionHandler maps this to HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}