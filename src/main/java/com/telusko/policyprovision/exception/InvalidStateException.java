package com.telusko.policyprovision.exception;

/**
 * Thrown when an operation is attempted against an entity that is in the
 * wrong state for it - e.g. submitting an already-submitted proposal, or
 * deleting a customer that still has proposals attached.
 * Mapped to HTTP 409 CONFLICT by GlobalExceptionHandler.
 */
public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String message) {
        super(message);
    }
}
