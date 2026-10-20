package com.bloodbank.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility for hashing and verifying passwords with SHA-256 and salt.
 */
public class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
        // Utility class
    }

    /**
     * Generates a 16-character lowercase hexadecimal salt.
     */
    public static String generateSalt() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Computes the lowercase hex SHA-256 hash of salt + password.
     */
    public static String hashPassword(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = salt + password;
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Constant-time comparison between stored hash and calculated hash.
     */
    public static boolean verifyPassword(String salt, String rawPassword, String expectedHash) {
        if (salt == null || rawPassword == null || expectedHash == null) {
            return false;
        }
        String calculated = hashPassword(salt, rawPassword);
        byte[] a = calculated.getBytes(StandardCharsets.UTF_8);
        byte[] b = expectedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
