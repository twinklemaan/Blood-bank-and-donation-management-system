package com.bloodbank.exception;

/**
 * Thrown when user credentials or authorization fails.
 */
public class AuthenticationException extends AppException {
    public AuthenticationException(String message) {
        super(message);
    }
}
