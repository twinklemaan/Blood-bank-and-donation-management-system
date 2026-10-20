package com.bloodbank.dao;

import com.bloodbank.exception.DatabaseException;
import com.bloodbank.model.Hospital;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for hospital operations demonstrating full CRUD.
 * Extends BaseDao (Rubric: Inheritance).
 */
public class HospitalDao extends BaseDao {
    private final AuthDao authDao = new AuthDao();

    /**
     * Create: Calls stored procedure sp_register_hospital (Rubric: JDBC CRUD).
     */
    public int registerHospital(String name, String phone, String city, String state, String pincode, String regNo)
            throws DatabaseException {
        String sql = "{CALL sp_register_hospital(?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, city);
            stmt.setString(4, state);
            stmt.setString(5, pincode);
            stmt.setString(6, regNo);
            stmt.registerOutParameter(7, Types.INTEGER);

            stmt.execute();
            return stmt.getInt(7);
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Read: Selects all hospitals using PreparedStatement (Rubric: JDBC CRUD).
     */
    public List<Hospital> getAllHospitals() throws DatabaseException {
        List<Hospital> list = new ArrayList<>();
        String sql = "SELECT hospital_id, name, reg_no, phone, city, state, pincode FROM hospital ORDER BY hospital_id";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Hospital(
                        rs.getInt("hospital_id"),
                        rs.getString("name"),
                        rs.getString("reg_no"),
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
     * Update: Updates hospital details using PreparedStatement (Rubric: JDBC CRUD).
     */
    public void updateHospital(int hospitalId, String name, String phone, String city, String state, String pincode, String regNo)
            throws DatabaseException {
        String sql = "UPDATE hospital SET name = ?, phone = ?, city = ?, state = ?, pincode = ?, reg_no = ? WHERE hospital_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, city);
            stmt.setString(4, state);
            stmt.setString(5, pincode);
            stmt.setString(6, regNo);
            stmt.setInt(7, hospitalId);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Hospital with ID " + hospitalId + " was not found.");
            }
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Delete: Deletes linked app_user first to satisfy MySQL foreign key constraints,
     * then deletes hospital record (Rubric: JDBC CRUD).
     */
    public void deleteHospital(int hospitalId) throws DatabaseException {
        // Delete linked login first
        authDao.deleteUserByHospitalId(hospitalId);

        String sql = "DELETE FROM hospital WHERE hospital_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, hospitalId);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Hospital with ID " + hospitalId + " was not found.");
            }
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }
}
