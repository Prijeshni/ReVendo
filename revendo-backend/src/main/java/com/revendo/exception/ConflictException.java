package com.revendo.exception;

/**
 * Thrown when a request conflicts with the current resource state
 * (e.g., buying a Sold listing, submitting a duplicate offer).
 * GlobalExceptionHandler maps this to HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}