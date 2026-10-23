package com.bloodbank.dao;

import com.bloodbank.exception.DatabaseException;
import com.bloodbank.model.StaffMember;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for staff operations demonstrating full CRUD.
 * Extends BaseDao (Rubric: Inheritance).
 */
public class StaffDao extends BaseDao {
    private final AuthDao authDao = new AuthDao();

    /**
     * Create: Calls stored procedure sp_register_staff (Rubric: JDBC CRUD).
     */
    public int registerStaff(int bankId, String name, String role, String phone) throws DatabaseException {
        String sql = "{CALL sp_register_staff(?, ?, ?, ?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, bankId);
            stmt.setString(2, name);
            stmt.setString(3, role);
            stmt.setString(4, phone);
            stmt.registerOutParameter(5, Types.INTEGER);

            stmt.execute();
            return stmt.getInt(5);
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Read: Selects all staff members across banks (Rubric: JDBC CRUD).
     */
    public List<StaffMember> getAllStaff() throws DatabaseException {
        List<StaffMember> list = new ArrayList<>();
        String sql = """
            SELECT s.staff_id, s.bank_id, b.name AS bank_name, s.name, s.role, s.phone
              FROM staff s
              JOIN blood_bank b ON s.bank_id = b.bank_id
             ORDER BY s.staff_id
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new StaffMember(
                        rs.getInt("staff_id"),
                        rs.getInt("bank_id"),
                        rs.getString("bank_name"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getString("phone")
                ));
            }
            return list;
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Read: Selects staff members for a specific blood bank.
     */
    public List<StaffMember> getStaffByBank(int bankId) throws DatabaseException {
        List<StaffMember> list = new ArrayList<>();
        String sql = """
            SELECT s.staff_id, s.bank_id, b.name AS bank_name, s.name, s.role, s.phone
              FROM staff s
              JOIN blood_bank b ON s.bank_id = b.bank_id
             WHERE s.bank_id = ?
             ORDER BY s.staff_id
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bankId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new StaffMember(
                            rs.getInt("staff_id"),
                            rs.getInt("bank_id"),
                            rs.getString("bank_name"),
                            rs.getString("name"),
                            rs.getString("role"),
                            rs.getString("phone")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Update: Updates staff details using PreparedStatement (Rubric: JDBC CRUD).
     */
    public void updateStaff(int staffId, int bankId, String name, String role, String phone) throws DatabaseException {
        String sql = "UPDATE staff SET bank_id = ?, name = ?, role = ?, phone = ? WHERE staff_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bankId);
            stmt.setString(2, name);
            stmt.setString(3, role);
            stmt.setString(4, phone);
            stmt.setInt(5, staffId);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Staff member with ID " + staffId + " was not found.");
            }
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Delete: Deletes linked app_user first to satisfy MySQL foreign key constraints,
     * then deletes staff record (Rubric: JDBC CRUD).
     */
    public void deleteStaff(int staffId) throws DatabaseException {
        // Delete linked user credentials first
        authDao.deleteUserByStaffId(staffId);

        String sql = "DELETE FROM staff WHERE staff_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, staffId);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Staff member with ID " + staffId + " was not found.");
            }
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }
}
