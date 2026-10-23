package com.bloodbank.dao;

import com.bloodbank.exception.AppException;
import com.bloodbank.exception.AuthenticationException;
import com.bloodbank.exception.DatabaseException;
import com.bloodbank.model.AdminUser;
import com.bloodbank.model.BankStaffUser;
import com.bloodbank.model.HospitalUser;
import com.bloodbank.model.User;
import com.bloodbank.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Data Access Object for user authentication and user management in app_user.
 * Extends BaseDao (Rubric: Inheritance).
 */
public class AuthDao extends BaseDao {

    /**
     * Authenticates a user against the app_user table and returns a concrete User instance.
     */
    public User authenticate(String username, String password) throws AppException {
        String sql = """
            SELECT u.user_id, u.username, u.salt, u.password_hash, u.role,
                   u.staff_id, u.hospital_id,
                   s.name AS staff_name, s.bank_id, b.name AS bank_name,
                   h.name AS hospital_name
              FROM app_user u
              LEFT JOIN staff s ON u.staff_id = s.staff_id
              LEFT JOIN blood_bank b ON s.bank_id = b.bank_id
              LEFT JOIN hospital h ON u.hospital_id = h.hospital_id
             WHERE u.username = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new AuthenticationException("Invalid username or password.");
                }

                int userId = rs.getInt("user_id");
                String foundUsername = rs.getString("username");
                String salt = rs.getString("salt");
                String passwordHash = rs.getString("password_hash");
                String role = rs.getString("role");

                if (!PasswordUtil.verifyPassword(salt, password, passwordHash)) {
                    throw new AuthenticationException("Invalid username or password.");
                }

                return switch (role) {
                    case "ADMIN" -> new AdminUser(userId, foundUsername);
                    case "STAFF" -> {
                        int staffId = rs.getInt("staff_id");
                        int bankId = rs.getInt("bank_id");
                        String bankName = rs.getString("bank_name");
                        String staffName = rs.getString("staff_name");
                        yield new BankStaffUser(userId, foundUsername, staffId, bankId, bankName, staffName);
                    }
                    case "HOSPITAL" -> {
                        int hospitalId = rs.getInt("hospital_id");
                        String hospitalName = rs.getString("hospital_name");
                        yield new HospitalUser(userId, foundUsername, hospitalId, hospitalName);
                    }
                    default -> throw new AuthenticationException("Unrecognized role: " + role);
                };
            }
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Creates a new user record in app_user with a salted SHA-256 hash.
     */
    public void createUser(String username, String rawPassword, String role, Integer staffId, Integer hospitalId)
            throws AppException {
        String sql = """
            INSERT INTO app_user (username, salt, password_hash, role, staff_id, hospital_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(salt, rawPassword);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, salt);
            stmt.setString(3, hash);
            stmt.setString(4, role);

            if (staffId != null) {
                stmt.setInt(5, staffId);
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            if (hospitalId != null) {
                stmt.setInt(6, hospitalId);
            } else {
                stmt.setNull(6, Types.INTEGER);
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Deletes user account linked to a specific staff member.
     * Must be called before deleting the staff record to avoid FK check constraint conflicts.
     */
    public void deleteUserByStaffId(int staffId) throws DatabaseException {
        String sql = "DELETE FROM app_user WHERE staff_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, staffId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Deletes user account linked to a specific hospital.
     * Must be called before deleting the hospital record to avoid FK check constraint conflicts.
     */
    public void deleteUserByHospitalId(int hospitalId) throws DatabaseException {
        String sql = "DELETE FROM app_user WHERE hospital_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, hospitalId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }
}
