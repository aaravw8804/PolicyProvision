package com.telusko.policyprovision.exception;

/**
 * Thrown when a request violates a business rule (age range, sum assured
 * range, PAN requirement, nominee rule, invalid reference value, etc).
 * Mapped to HTTP 400 by GlobalExceptionHandler.
 */
public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }
}
