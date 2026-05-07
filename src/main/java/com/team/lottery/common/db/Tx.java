package com.team.lottery.common.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Хелпер для работы с транзакциями. Заменяет ручной boilerplate
 * setAutoCommit(false) → commit/rollback → setAutoCommit(true).
 *
 * Использование:
 *   Tx.execute(ds, c -> {
 *       repo.someUpdate(c, ...);
 *       repo.anotherUpdate(c, ...);
 *   });
 *
 *   long id = Tx.execute(ds, c -> {
 *       return repo.insert(c, ...);
 *   });
 *
 * RuntimeException пробрасывается как есть; checked-исключение
 * заворачивается в RuntimeException.
 */
public final class Tx {

    private Tx() {
    }

    public static <T> T execute(DataSource ds, SqlFunction<Connection, T> work) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(c);
                c.commit();
                return result;
            } catch (Exception e) {
                try {
                    c.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(e);
            } finally {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // не критично — connection возвращается в Hikari пул и сбрасывается
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error in transaction", e);
        }
    }

    public static void execute(DataSource ds, SqlConsumer<Connection> work) {
        execute(ds, c -> {
            work.accept(c);
            return null;
        });
    }

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface SqlConsumer<T> {
        void accept(T t) throws Exception;
    }
}
