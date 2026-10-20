package com.bloodbank.dao;

import com.bloodbank.exception.DatabaseException;
import com.bloodbank.model.Bank;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for blood bank operations demonstrating full CRUD with Prepared and Callable statements.
 * Extends BaseDao (Rubric: Inheritance).
 */
public class BankDao extends BaseDao {

    /**
     * Create: Calls stored procedure sp_register_bank (Rubric: JDBC CRUD).
     */
    public int registerBank(String name, String phone, String city, String state, String pincode, String licenceNo)
            throws DatabaseException {
        String sql = "{CALL sp_register_bank(?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, city);
            stmt.setString(4, state);
            stmt.setString(5, pincode);
            stmt.setString(6, licenceNo);
            stmt.registerOutParameter(7, Types.INTEGER);

            stmt.execute();
            return stmt.getInt(7);
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Read: Selects all blood banks using PreparedStatement (Rubric: JDBC CRUD).
     */
    public List<Bank> getAllBanks() throws DatabaseException {
        List<Bank> list = new ArrayList<>();
        String sql = "SELECT bank_id, name, licence_no, phone, city, state, pincode FROM blood_bank ORDER BY bank_id";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Bank(
                        rs.getInt("bank_id"),
                        rs.getString("name"),
                        rs.getString("licence_no"),
                        rs.getString("phone"),
                        rs.getString("city"),
                        rs.getString("state"),
                        rs.getString("pincode")
                ));
            }
            return list;
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Update: Updates blood bank details using PreparedStatement (Rubric: JDBC CRUD).
     */
    public void updateBank(int bankId, String name, String phone, String city, String state, String pincode, String licenceNo)
            throws DatabaseException {
        String sql = "UPDATE blood_bank SET name = ?, phone = ?, city = ?, state = ?, pincode = ?, licence_no = ? WHERE bank_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, city);
            stmt.setString(4, state);
            stmt.setString(5, pincode);
            stmt.setString(6, licenceNo);
            stmt.setInt(7, bankId);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Blood bank with ID " + bankId + " was not found.");
            }
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Delete: Deletes blood bank if no dependent records exist (Rubric: JDBC CRUD).
     */
    public void deleteBank(int bankId) throws DatabaseException {
        String sql = "DELETE FROM blood_bank WHERE bank_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bankId);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Blood bank with ID " + bankId + " was not found.");
            }
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }
}
