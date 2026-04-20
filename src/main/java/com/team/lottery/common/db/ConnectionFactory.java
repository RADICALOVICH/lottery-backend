package com.team.lottery.common.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class ConnectionFactory {

    private static DataSource dataSource;

    private ConnectionFactory() {
    }

    public static void init(DataSource ds) {
        dataSource = ds;
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("ConnectionFactory is not initialized");
        }
        return dataSource.getConnection();
    }
}