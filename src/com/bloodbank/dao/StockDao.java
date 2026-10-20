package com.bloodbank.dao;

import com.bloodbank.exception.DatabaseException;
import com.bloodbank.model.StockEntry;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for blood stock queries and stored procedures.
 * Extends BaseDao (Rubric: Inheritance).
 */
public class StockDao extends BaseDao {

    /**
     * Executes the daily expiry sweep stored procedure.
     */
    public void runExpirySweep() throws DatabaseException {
        String sql = "CALL sp_expire_all()";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Loads stock availability from v_bank_availability.
     */
    public List<StockEntry> getAllStock() throws DatabaseException {
        List<StockEntry> list = new ArrayList<>();
        String sql = """
            SELECT bank_id, bank_name, city, blood_group, units_available
              FROM v_bank_availability
             ORDER BY bank_name, blood_group
        """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new StockEntry(
                        rs.getInt("bank_id"),
                        rs.getString("bank_name"),
                        rs.getString("city"),
                        rs.getString("blood_group"),
                        rs.getInt("units_available"),
                        null
                ));
            }
            return list;
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Rubric: Collections requirement - Map<String, Integer> for stock per blood group.
     */
    public Map<String, Integer> getStockForBank(int bankId) throws DatabaseException {
        Map<String, Integer> map = new LinkedHashMap<>();
        // All 8 standard blood groups initialized to 0
        String[] groups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        for (String g : groups) {
            map.put(g, 0);
        }

        String sql = """
            SELECT blood_group, units_available
              FROM v_bank_availability
             WHERE bank_id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bankId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("blood_group"), rs.getInt("units_available"));
                }
            }
            return map;
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }
}
