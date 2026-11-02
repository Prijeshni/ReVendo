package com.revendo.exception;

/**
 * Thrown when a requested resource (Listing, User, etc.) cannot be found.
 * GlobalExceptionHandler maps this to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " with id " + id + " was not found.");
    }
}