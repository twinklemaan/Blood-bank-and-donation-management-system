package com.bloodbank.util;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Translates low-level SQLExceptions into user-friendly messages.
 */
public class ErrorMapper {
    private static final Logger LOGGER = Logger.getLogger(ErrorMapper.class.getName());

    private ErrorMapper() {
        // Utility class
    }

    public static String toFriendly(SQLException e) {
        LOGGER.log(Level.WARNING, "Database error: SQLState={0}, ErrorCode={1}, Message={2}",
                new Object[]{e.getSQLState(), e.getErrorCode(), e.getMessage()});

        String sqlState = e.getSQLState() != null ? e.getSQLState() : "";
        int errorCode = e.getErrorCode();
        String message = e.getMessage() != null ? e.getMessage() : "";

        // Custom triggers and procedures use SQLSTATE 45000 with explicit descriptive messages
        if ("45000".equals(sqlState)) {
            return message;
        }

        // Duplicate entry (e.g. unique licence, registration, username)
        if (errorCode == 1062) {
            return "That licence/registration number or username already exists.";
        }

        // Foreign key constraint failure (e.g. deleting parent record still in use)
        if (errorCode == 1451 || errorCode == 1452) {
            return "This record is in use / refers to something that does not exist.";
        }

        // CHECK constraint violation (MySQL 8.0.16+)
        if (errorCode == 3819) {
            String lowerMsg = message.toLowerCase();
            if (lowerMsg.contains("ck_don_hb")) {
                return "Haemoglobin must be between 5 and 25 g/dL.";
            } else if (lowerMsg.contains("pin")) {
                return "Pincode must be exactly 6 digits.";
            } else if (lowerMsg.contains("ck_don_units")) {
                return "Units must be greater than 0.";
            } else if (lowerMsg.contains("ck_req_units")) {
                return "Units requested must be greater than 0.";
            } else if (lowerMsg.contains("ck_donor_gender")) {
                return "Gender must be 'M', 'F', or 'O'.";
            } else if (lowerMsg.contains("bg")) {
                return "Invalid blood group specified.";
            } else if (lowerMsg.contains("urgency")) {
                return "Urgency must be Normal, Urgent, or Critical.";
            }
            return "The input violates a database constraint.";
        }

        // Connection failure
        if (sqlState.startsWith("08") || errorCode == 0
                || message.toLowerCase().contains("communications link failure")
                || message.toLowerCase().contains("access denied")) {
            return "Cannot reach the database. Check config/db.properties and that MySQL is running.";
        }

        return message.isEmpty() ? "An unexpected database error occurred." : message;
    }
}
