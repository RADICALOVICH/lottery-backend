package com.team.lottery.common.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Тонкая обёртка над JDBC. Репозитории вызывают отсюда withConnection/withTx,
 * чтобы не повторять ритуал открытия соединения и управления транзакцией.
 *
 * SQLException всегда заворачивается в RuntimeException — сервисы и контроллеры
 * не обязаны знать о чекед-исключениях JDBC. ErrorHandler превратит такую ошибку
 * в 500 INTERNAL_ERROR.
 *
 * Примечание по PG ENUM: здесь нет логики кастов — они пишутся прямо в SQL
 * репозиториев (например, "?::user_role"). См. HANDOFF.md 4.1.
 */
public final class JdbcHelper {

    private JdbcHelper() {
    }

    /**
     * Выдаёт соединение из пула, закрывает после выполнения work.
     * Автокоммит оставляет как есть (по умолчанию true в Hikari).
     */
    public static <T> T withConnection(DataSource ds, SqlFunction<Connection, T> work) {
        try (Connection c = ds.getConnection()) {
            return work.apply(c);
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    /**
     * Выполняет work в одной транзакции. Успех — commit, любое исключение — rollback.
     * Оригинальное RuntimeException (включая ApiException) пробрасывается без обёртки,
     * чтобы ErrorHandler вернул корректный HTTP-код клиенту.
     */
    public static <T> T withTx(DataSource ds, SqlFunction<Connection, T> work) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(c);
                c.commit();
                return result;
            } catch (Exception e) {
                try {
                    c.rollback();
                } catch (SQLException rb) {
                    e.addSuppressed(rb);
                }
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    /**
     * SELECT → список объектов. Параметры выставляются через setter, строки маппятся через mapper.
     */
    public static <T> List<T> query(
            Connection c,
            String sql,
            ParamSetter setter,
            RowMapper<T> mapper
    ) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        }
    }

    /**
     * UPDATE/DELETE/INSERT без RETURNING. Возвращает количество затронутых строк.
     */
    public static int update(Connection c, String sql, ParamSetter setter) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            return ps.executeUpdate();
        }
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
