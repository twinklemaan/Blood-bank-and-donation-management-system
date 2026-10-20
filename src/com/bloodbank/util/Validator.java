package com.bloodbank.util;

import com.bloodbank.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Client-side input validation for user convenience.
 * Database constraints and triggers remain the authoritative rules.
 */
public class Validator {
    private static final Set<String> VALID_BLOOD_GROUPS = Set.of(
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    );

    private static final Set<String> VALID_GENDERS = Set.of("M", "F", "O");

    private static final Set<String> VALID_URGENCIES = Set.of("Normal", "Urgent", "Critical");

    private Validator() {
        // Utility class
    }

    public static void requireNotEmpty(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty.");
        }
    }

    public static void validatePincode(String pincode) throws ValidationException {
        requireNotEmpty(pincode, "Pincode");
        if (!pincode.trim().matches("^[0-9]{6}$")) {
            throw new ValidationException("Pincode must be exactly 6 digits.");
        }
    }

    public static LocalDate validateDate(String dateStr, String fieldName) throws ValidationException {
        requireNotEmpty(dateStr, fieldName);
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldName + " must be in YYYY-MM-DD format.");
        }
    }

    public static int validatePositiveInt(String value, String fieldName) throws ValidationException {
        requireNotEmpty(value, fieldName);
        try {
            int val = Integer.parseInt(value.trim());
            if (val <= 0) {
                throw new ValidationException(fieldName + " must be greater than 0.");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid number.");
        }
    }

    public static double validatePositiveDouble(String value, String fieldName) throws ValidationException {
        requireNotEmpty(value, fieldName);
        try {
            double val = Double.parseDouble(value.trim());
            if (val <= 0) {
                throw new ValidationException(fieldName + " must be greater than 0.");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid decimal number.");
        }
    }

    public static void validateBloodGroup(String group) throws ValidationException {
        requireNotEmpty(group, "Blood Group");
        if (!VALID_BLOOD_GROUPS.contains(group.trim().toUpperCase())) {
            throw new ValidationException("Blood group must be one of: A+, A-, B+, B-, AB+, AB-, O+, O-");
        }
    }

    public static void validateGender(String gender) throws ValidationException {
        requireNotEmpty(gender, "Gender");
        if (!VALID_GENDERS.contains(gender.trim().toUpperCase())) {
            throw new ValidationException("Gender must be 'M', 'F', or 'O'.");
        }
    }

    public static void validateUrgency(String urgency) throws ValidationException {
        requireNotEmpty(urgency, "Urgency");
        if (!VALID_URGENCIES.contains(urgency.trim())) {
            throw new ValidationException("Urgency must be Normal, Urgent, or Critical.");
        }
    }
}
