package com.revendo.exception;

/**
 * Thrown when a business rule validation fails (duplicate email,
 * invalid phone number, password too short, etc.).
 * GlobalExceptionHandler maps this to HTTP 400 Bad Request.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}