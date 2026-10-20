package com.bloodbank.exception;

/**
 * Thrown when a database operation fails, encapsulating friendly error messages.
 */
public class DatabaseException extends AppException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
