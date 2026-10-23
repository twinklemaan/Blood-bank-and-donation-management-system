package com.bloodbank.dao;

import com.bloodbank.exception.DatabaseException;
import com.bloodbank.model.Donation;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for blood donations and laboratory screening.
 * Demonstrates PreparedStatement and CallableStatement usage (Rubric: JDBC CRUD).
 */
public class DonationDao extends BaseDao {

    /**
     * Create: Calls stored procedure sp_record_donation (Rubric: JDBC CRUD).
     * Always initially recorded as 'Pending'.
     */
    public int recordDonation(int donorId, int bankId, int units, BigDecimal hb, String result, LocalDate donationDate)
            throws DatabaseException {
        String sql = "{CALL sp_record_donation(?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, donorId);
            stmt.setInt(2, bankId);
            stmt.setInt(3, units);
            stmt.setBigDecimal(4, hb);
            stmt.setString(5, result != null ? result : "Pending");

            if (donationDate != null) {
                stmt.setDate(6, Date.valueOf(donationDate));
            } else {
                stmt.setNull(6, Types.DATE);
            }

            stmt.registerOutParameter(7, Types.INTEGER);
            stmt.execute();
            return stmt.getInt(7);
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Update: Calls stored procedure sp_update_screening (Rubric: JDBC CRUD).
     * Transitions screening from 'Pending' to 'Pass' or 'Fail'.
     */
    public void updateScreening(int donationId, String result) throws DatabaseException {
        String sql = "{CALL sp_update_screening(?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, donationId);
            stmt.setString(2, result);
            stmt.execute();
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Read: Selects donations for a specific bank from v_donation_detail.
     */
    public List<Donation> getDonationsByBank(int bankId) throws DatabaseException {
        List<Donation> list = new ArrayList<>();
        String sql = """
            SELECT n.donation_id, n.donor_id, d.name AS donor_name, n.blood_group,
                   n.bank_id, b.name AS bank_name, n.donation_date, n.expiry_date,
                   n.units, n.units_remaining, n.hemoglobin_level, n.screening_result
              FROM v_donation_detail n
              JOIN donor d ON d.donor_id = n.donor_id
              JOIN blood_bank b ON b.bank_id = n.bank_id
             WHERE n.bank_id = ?
             ORDER BY n.donation_id DESC
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bankId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Read: Selects Pending donations for screening at a specific bank.
     */
    public List<Donation> getPendingDonationsByBank(int bankId) throws DatabaseException {
        List<Donation> list = new ArrayList<>();
        String sql = """
            SELECT n.donation_id, n.donor_id, d.name AS donor_name, n.blood_group,
                   n.bank_id, b.name AS bank_name, n.donation_date, n.expiry_date,
                   n.units, n.units_remaining, n.hemoglobin_level, n.screening_result
              FROM v_donation_detail n
              JOIN donor d ON d.donor_id = n.donor_id
              JOIN blood_bank b ON b.bank_id = n.bank_id
             WHERE n.bank_id = ? AND n.screening_result = 'Pending'
             ORDER BY n.donation_id ASC
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bankId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Read: Selects all donations across banks with filters (Admin audit view).
     */
    public List<Donation> getAllDonations(String statusFilter, Integer bankIdFilter) throws DatabaseException {
        List<Donation> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT n.donation_id, n.donor_id, d.name AS donor_name, n.blood_group,
                   n.bank_id, b.name AS bank_name, n.donation_date, n.expiry_date,
                   n.units, n.units_remaining, n.hemoglobin_level, n.screening_result
              FROM v_donation_detail n
              JOIN donor d ON d.donor_id = n.donor_id
              JOIN blood_bank b ON b.bank_id = n.bank_id
             WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("All")) {
            sql.append(" AND n.screening_result = ?");
            params.add(statusFilter.trim());
        }
        if (bankIdFilter != null && bankIdFilter > 0) {
            sql.append(" AND n.bank_id = ?");
            params.add(bankIdFilter);
        }

        sql.append(" ORDER BY n.donation_id DESC");

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Delete: Deletes donation record (allowed for Pending/Fail; database trigger blocks deletion if Pass).
     */
    public void deleteDonation(int donationId) throws DatabaseException {
        String sql = "DELETE FROM donation WHERE donation_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, donationId);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Donation record #" + donationId + " was not found.");
            }
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    private Donation mapRow(ResultSet rs) throws SQLException {
        Date donDate = rs.getDate("donation_date");
        Date expDate = rs.getDate("expiry_date");
        return new Donation(
                rs.getInt("donation_id"),
                rs.getInt("donor_id"),
                rs.getString("donor_name"),
                rs.getString("blood_group"),
                rs.getInt("bank_id"),
                rs.getString("bank_name"),
                donDate != null ? donDate.toLocalDate() : null,
                expDate != null ? expDate.toLocalDate() : null,
                rs.getInt("units"),
                rs.getInt("units_remaining"),
                rs.getBigDecimal("hemoglobin_level"),
                rs.getString("screening_result")
        );
    }
}
