package com.bloodbank.exception;

/**
 * Thrown when client-side input validation fails.
 */
public class ValidationException extends AppException {
    public ValidationException(String message) {
        super(message);
    }
}
