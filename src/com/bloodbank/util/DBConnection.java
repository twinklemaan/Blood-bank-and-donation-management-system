package com.bloodbank.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages database connectivity.
 * Returns a new connection per operation so threads never share connections.
 * autoCommit remains true so database stored procedures manage their own transactions.
 */
public class DBConnection {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // Connector/J driver will still be loaded via ServiceLoader if present on classpath
        }
    }

    private DBConnection() {
        // Utility class
    }

    /**
     * Obtains a new database connection using configuration from AppConfig.
     */
    public static Connection getConnection() throws SQLException {
        AppConfig config = AppConfig.getInstance();
        Connection conn = DriverManager.getConnection(
                config.getDbUrl(),
                config.getDbUser(),
                config.getDbPassword()
        );
        conn.setAutoCommit(true);
        return conn;
    }
}
