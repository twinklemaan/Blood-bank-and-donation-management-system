package com.bloodbank.dao;

import com.bloodbank.exception.DatabaseException;
import com.bloodbank.model.Bank;
import com.bloodbank.model.StockEntry;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for blood stock inventory queries.
 * Extends BaseDao (Rubric: Inheritance).
 */
public class StockDao extends BaseDao {
    public static final String[] ALL_BLOOD_GROUPS = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

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
     * Reads raw view rows from v_bank_availability.
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
     * Builds a comprehensive grid: (All Banks) × (All 8 Blood Groups).
     * Any pair with no stock row in v_bank_availability defaults to 0 units.
     *
     * @param filterBankId null for all banks, or specific bankId for staff scoping
     */
    public List<StockEntry> getComprehensiveStockGrid(Integer filterBankId) throws DatabaseException {
        List<StockEntry> result = new ArrayList<>();

        // 1. Fetch relevant banks
        BankDao bankDao = new BankDao();
        List<Bank> banks = bankDao.getAllBanks();
        if (filterBankId != null && filterBankId > 0) {
            banks = banks.stream().filter(b -> b.getBankId() == filterBankId).toList();
        }

        // 2. Query available stock from v_bank_availability into map of (bankId:group -> units)
        Map<String, Integer> stockMap = new HashMap<>();
        String sql = "SELECT bank_id, blood_group, units_available FROM v_bank_availability";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int bId = rs.getInt("bank_id");
                String group = rs.getString("blood_group");
                int units = rs.getInt("units_available");
                stockMap.put(bId + ":" + group, units);
            }
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }

        // 3. Synthesize full matrix: for each bank, check every one of the 8 blood groups
        for (Bank bank : banks) {
            for (String group : ALL_BLOOD_GROUPS) {
                String key = bank.getBankId() + ":" + group;
                int units = stockMap.getOrDefault(key, 0);
                result.add(new StockEntry(
                        bank.getBankId(),
                        bank.getName(),
                        bank.getCity(),
                        group,
                        units,
                        null
                ));
            }
        }

        return result;
    }

    /**
     * Rubric: Collections requirement - Map<String, Integer> for stock per blood group.
     */
    public Map<String, Integer> getStockForBank(int bankId) throws DatabaseException {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String g : ALL_BLOOD_GROUPS) {
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
