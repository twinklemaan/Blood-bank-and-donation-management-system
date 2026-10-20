package com.bloodbank.dao;

import com.bloodbank.exception.DatabaseException;
import com.bloodbank.model.Donor;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data Access Object for donor registrations and status queries.
 * Extends BaseDao (Rubric: Inheritance).
 */
public class DonorDao extends BaseDao {

    /**
     * Create: Calls stored procedure sp_register_donor (Rubric: JDBC CRUD).
     */
    public int registerDonor(String name, String gender, LocalDate dob, String bloodGroup,
                             String city, String state, String pincode, String phone) throws DatabaseException {
        String sql = "{CALL sp_register_donor(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, gender);
            stmt.setDate(3, Date.valueOf(dob));
            stmt.setString(4, bloodGroup);
            stmt.setString(5, city);
            stmt.setString(6, state);
            stmt.setString(7, pincode);
            stmt.setString(8, phone);
            stmt.registerOutParameter(9, Types.INTEGER);

            stmt.execute();
            return stmt.getInt(9);
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Read: Selects donors with age and next eligible date from v_donor_status, with optional search filters.
     */
    public List<Donor> searchDonors(String bloodGroupFilter, String citySearch) throws DatabaseException {
        List<Donor> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT d.donor_id, d.name, d.gender, d.dob, d.blood_group, d.city, d.state, d.pincode,
                   v.age, v.next_eligible_date,
                   GROUP_CONCAT(p.phone SEPARATOR ', ') AS phones
              FROM donor d
              JOIN v_donor_status v ON d.donor_id = v.donor_id
              LEFT JOIN donor_phone p ON d.donor_id = p.donor_id
             WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (bloodGroupFilter != null && !bloodGroupFilter.trim().isEmpty() && !bloodGroupFilter.equalsIgnoreCase("All")) {
            sql.append(" AND d.blood_group = ?");
            params.add(bloodGroupFilter.trim());
        }
        if (citySearch != null && !citySearch.trim().isEmpty()) {
            sql.append(" AND LOWER(d.city) LIKE ?");
            params.add("%" + citySearch.trim().toLowerCase() + "%");
        }

        sql.append(" GROUP BY d.donor_id, d.name, d.gender, d.dob, d.blood_group, d.city, d.state, d.pincode, v.age, v.next_eligible_date");
        sql.append(" ORDER BY d.donor_id DESC");

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String phoneConcat = rs.getString("phones");
                    List<String> phoneList = phoneConcat != null ? Arrays.asList(phoneConcat.split(",\\s*")) : List.of();
                    Date nextDateSql = rs.getDate("next_eligible_date");
                    LocalDate nextDate = nextDateSql != null ? nextDateSql.toLocalDate() : null;

                    list.add(new Donor(
                            rs.getInt("donor_id"),
                            rs.getString("name"),
                            rs.getString("gender"),
                            rs.getDate("dob").toLocalDate(),
                            rs.getString("blood_group"),
                            rs.getString("city"),
                            rs.getString("state"),
                            rs.getString("pincode"),
                            phoneList,
                            rs.getInt("age"),
                            nextDate
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    /**
     * Checks eligibility of a donor on a specified date using stored functions fn_is_eligible and fn_next_eligible_date.
     */
    public EligibilityResult checkEligibility(int donorId, LocalDate onDate) throws DatabaseException {
        String sql = "SELECT fn_is_eligible(?, ?) AS is_eligible, fn_next_eligible_date(?) AS next_date";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, donorId);
            stmt.setDate(2, Date.valueOf(onDate != null ? onDate : LocalDate.now()));
            stmt.setInt(3, donorId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean eligible = rs.getInt("is_eligible") == 1;
                    Date nextSql = rs.getDate("next_date");
                    LocalDate nextDate = nextSql != null ? nextSql.toLocalDate() : null;
                    return new EligibilityResult(eligible, nextDate);
                }
            }
            return new EligibilityResult(false, null);
        } catch (SQLException e) {
            throw wrapSqlException(e);
        }
    }

    public static class EligibilityResult {
        private final boolean eligible;
        private final LocalDate nextEligibleDate;

        public EligibilityResult(boolean eligible, LocalDate nextEligibleDate) {
            this.eligible = eligible;
            this.nextEligibleDate = nextEligibleDate;
        }

        public boolean isEligible() {
            return eligible;
        }

        public LocalDate getNextEligibleDate() {
            return nextEligibleDate;
        }
    }
}
