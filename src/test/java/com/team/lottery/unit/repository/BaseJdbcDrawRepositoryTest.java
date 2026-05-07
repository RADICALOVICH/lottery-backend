package com.team.lottery.unit.repository;


import com.team.lottery.common.db.Tx;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawJdbcRepository;
import com.team.lottery.support.TestPostgres;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public abstract class BaseJdbcDrawRepositoryTest {

    protected static DataSource dataSource;
    protected DrawJdbcRepository repository;
    protected Long testUserId;

    @BeforeAll
    static void setupDataSource() {
        // Используем общий Testcontainer (TestPostgres). Миграции Flyway уже
        // прогнаны в TestPostgres static {} init — здесь просто строим пул.
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(TestPostgres.INSTANCE.getJdbcUrl());
        config.setUsername(TestPostgres.INSTANCE.getUsername());
        config.setPassword(TestPostgres.INSTANCE.getPassword());
        dataSource = new HikariDataSource(config);
    }

    @BeforeEach
    void setUp() {
        repository = new DrawJdbcRepository(dataSource);

        // Очищаем таблицы перед каждым тестом (CASCADE удалит и draws, и билеты)
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("TRUNCATE TABLE users, draws RESTART IDENTITY CASCADE");

            // Создаем тестового пользователя, так как draws.created_by требует валидный FK
            testUserId = createTestUser(conn);
        } catch (Exception e) {
            throw new RuntimeException("Setup failed", e);
        }
    }

    // --- Вспомогательные методы ---

    protected Long createTestUser(Connection conn) throws Exception {
        String sql = "INSERT INTO users (login, password_hash, role) VALUES (?, ?, CAST(? AS user_role)) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "test_admin_" + System.currentTimeMillis());
            ps.setString(2, "hash");
            ps.setString(3, "ADMIN");
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getLong(1);
        }
    }

    protected Draw saveCustomDraw(String title, DrawStatus status) {
        Draw draw = new Draw(
                null,
                title,
                null,
                OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MICROS),
                100,
                testUserId,
                null
        );

        Draw saved = Tx.execute(dataSource, c -> {
            return repository.save(c, draw);
        });
        Tx.execute(dataSource, c -> {
            repository.updateStatus(c, saved.id(), status);
        });
        return repository.findById(saved.id()).orElseThrow();
    }

    protected void forceUpdateEndDate(Long id, OffsetDateTime date) {
        try (Connection conn = dataSource.getConnection()) {
            var ps = conn.prepareStatement("UPDATE draws SET end_date = ? WHERE id = ?");
            ps.setObject(1, date);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
