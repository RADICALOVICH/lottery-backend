//TO DO: delete file, not used!

/*
package com.team.lottery.common.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcHelper {

    private static final String DB_ERROR_MESSAGE = "Database error";

    public static final ParamSetter NO_PARAMS = ps -> {
    };

    private JdbcHelper() {
    }

    public static <T> T withConnection(DataSource ds, SqlFunction<Connection, T> work) {
        try (Connection c = ds.getConnection()) {
            return work.apply(c);
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    public static <T> T withTx(DataSource ds, SqlFunction<Connection, T> work) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(c);
                c.commit();
                return result;
            } catch (Exception e) {
                rollbackQuietly(c, e);
                throw propagate(e);
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /**
     * Legacy JDBC helper methods.
     *
     * They remain for backward compatibility with existing repositories.
     * Do not use them in new repositories.
     * New code should use plain JDBC directly inside repository methods.
     */

/*
    @Deprecated(forRemoval = false, since = "2026-04-26")
    public static <T> List<T> query(
            Connection c,
            String sql,
            ParamSetter setter,
            RowMapper<T> mapper
    ) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            safeSetter(setter).set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        }
    }
    @Deprecated(forRemoval = false, since = "2026-04-26")
    public static <T> List<T> query(
            Connection c,
            String sql,
            RowMapper<T> mapper
    ) throws SQLException {
        return query(c, sql, NO_PARAMS, mapper);
    }

    @Deprecated(forRemoval = false, since = "2026-04-26")
    public static int update(Connection c, String sql, ParamSetter setter) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            safeSetter(setter).set(ps);
            return ps.executeUpdate();
        }
    }

    @Deprecated(forRemoval = false, since = "2026-04-26")
    public static int update(Connection c, String sql) throws SQLException {
        return update(c, sql, NO_PARAMS);
    }

    private static ParamSetter safeSetter(ParamSetter setter) {
        return setter != null ? setter : NO_PARAMS;
    }


    private static void rollbackQuietly(Connection c, Exception originalException) {
        try {
            c.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private static RuntimeException propagate(Exception e) {
        if (e instanceof RuntimeException re) {
            return re;
        }
        return new RuntimeException(e);
    }

    private static RuntimeException dbError(SQLException e) {
        return new RuntimeException(DB_ERROR_MESSAGE, e);
    }

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T t) throws SQLException;
    }

    @FunctionalInterface
    public interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}

 */
