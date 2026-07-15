package com.telusko.policyprovision.exception;

/**
 * Thrown when a requested entity (Customer, Proposal, etc.) does not exist.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
