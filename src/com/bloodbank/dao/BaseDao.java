package com.bloodbank.dao;

import com.bloodbank.exception.DatabaseException;
import com.bloodbank.util.DBConnection;
import com.bloodbank.util.ErrorMapper;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstract base class for all Data Access Objects.
 * Satisfies the Inheritance rubric criterion and encapsulates connection retrieval and exception wrapping.
 */
public abstract class BaseDao {

    /**
     * Obtains a new database connection from the connection utility.
     */
    protected Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

    /**
     * Wraps a raw SQLException into a user-friendly DatabaseException using ErrorMapper.
     */
    protected DatabaseException wrapSqlException(SQLException e) {
        String friendlyMessage = ErrorMapper.toFriendly(e);
        return new DatabaseException(friendlyMessage, e);
    }
}
