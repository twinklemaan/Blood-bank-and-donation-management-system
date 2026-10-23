package com.bloodbank.dao;

import com.bloodbank.exception.DatabaseException;
import com.bloodbank.model.BloodRequest;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for blood requests and order fulfilment.
 * Demonstrates PreparedStatement and CallableStatement usage (Rubric: JDBC CRUD).
 */
public class RequestDao extends BaseDao {

    /**
     * Create: Calls stored procedure sp_raise_request (Rubric: JDBC CRUD).
     */
    public int raiseRequest(int hospitalId, int bankId, String bloodGroup, int units, String urgency)
            throws DatabaseException {
        String sql = "{CALL sp_raise_request(?, ?, ?, ?, ?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, hospitalId);
            stmt.setInt(2, bankId);
            stmt.setString(3, bloodGroup);
            stmt.setInt(4, units);
            stmt.setString(5, urgency);
            stmt.registerOutParameter(6, Types.INTEGER);

            stmt.execute();
            return stmt.getInt(6);
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Update: Calls stored procedure sp_fulfil_request (Rubric: JDBC CRUD).
     * Atomic check-and-deduct using FEFO and database row-locks.
     * Returns 'Fulfilled' or 'Rejected' (when stock is insufficient).
     */
    public String fulfilRequest(int requestId, int staffId, int holdSeconds) throws DatabaseException {
        String sql = "{CALL sp_fulfil_request(?, ?, ?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, requestId);
            stmt.setInt(2, staffId);
            stmt.setInt(3, holdSeconds);
            stmt.registerOutParameter(4, Types.VARCHAR);

            stmt.execute();
            return stmt.getString(4);
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Read: Selects Pending requests for a specific bank, sorted using Comparator (Rubric: Collections).
     */
    public List<BloodRequest> getPendingRequestsForBank(int bankId) throws DatabaseException {
        List<BloodRequest> list = new ArrayList<>();
        String sql = """
            SELECT r.request_id, r.hospital_id, h.name AS hospital_name,
                   r.bank_id, b.name AS bank_name, r.blood_group,
                   r.units_requested, r.urgency, r.status,
                   r.request_date, r.fulfilled_date
              FROM blood_request r
              JOIN hospital h ON h.hospital_id = r.hospital_id
              JOIN blood_bank b ON b.bank_id = r.bank_id
             WHERE r.bank_id = ? AND r.status = 'Pending'
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bankId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

            // Rubric: Collections requirement - sort requests Critical -> Urgent -> Normal, then oldest first
            list.sort(BloodRequest.BY_URGENCY_THEN_DATE);
            return list;
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Read: Selects all requests raised by a specific hospital.
     */
    public List<BloodRequest> getRequestsByHospital(int hospitalId) throws DatabaseException {
        List<BloodRequest> list = new ArrayList<>();
        String sql = """
            SELECT r.request_id, r.hospital_id, h.name AS hospital_name,
                   r.bank_id, b.name AS bank_name, r.blood_group,
                   r.units_requested, r.urgency, r.status,
                   r.request_date, r.fulfilled_date
              FROM blood_request r
              JOIN hospital h ON h.hospital_id = r.hospital_id
              JOIN blood_bank b ON b.bank_id = r.bank_id
             WHERE r.hospital_id = ?
             ORDER BY r.request_id DESC
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, hospitalId);
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
     * Read: Selects all requests across hospitals with optional filters (Admin audit view).
     */
    public List<BloodRequest> getAllRequests(String statusFilter, Integer bankIdFilter, Integer hospitalIdFilter)
            throws DatabaseException {
        List<BloodRequest> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT r.request_id, r.hospital_id, h.name AS hospital_name,
                   r.bank_id, b.name AS bank_name, r.blood_group,
                   r.units_requested, r.urgency, r.status,
                   r.request_date, r.fulfilled_date
              FROM blood_request r
              JOIN hospital h ON h.hospital_id = r.hospital_id
              JOIN blood_bank b ON b.bank_id = r.bank_id
             WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("All")) {
            sql.append(" AND r.status = ?");
            params.add(statusFilter.trim());
        }
        if (bankIdFilter != null && bankIdFilter > 0) {
            sql.append(" AND r.bank_id = ?");
            params.add(bankIdFilter);
        }
        if (hospitalIdFilter != null && hospitalIdFilter > 0) {
            sql.append(" AND r.hospital_id = ?");
            params.add(hospitalIdFilter);
        }

        sql.append(" ORDER BY r.request_id DESC");

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

    private BloodRequest mapRow(ResultSet rs) throws SQLException {
        Timestamp reqTs = rs.getTimestamp("request_date");
        Timestamp fulTs = rs.getTimestamp("fulfilled_date");

        LocalDateTime reqDate = reqTs != null ? reqTs.toLocalDateTime() : null;
        LocalDateTime fulDate = fulTs != null ? fulTs.toLocalDateTime() : null;

        return new BloodRequest(
                rs.getInt("request_id"),
                rs.getInt("hospital_id"),
                rs.getString("hospital_name"),
                rs.getInt("bank_id"),
                rs.getString("bank_name"),
                rs.getString("blood_group"),
                rs.getInt("units_requested"),
                rs.getString("urgency"),
                rs.getString("status"),
                reqDate,
                fulDate
        );
    }
}
