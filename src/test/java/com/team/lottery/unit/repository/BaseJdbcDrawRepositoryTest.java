package com.team.lottery.unit.repository;


import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawJdbcRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Testcontainers
public abstract class BaseJdbcDrawRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("lottery_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    protected static DataSource dataSource;
    protected DrawJdbcRepository repository;
    protected Long testUserId;

    @BeforeAll
    static void setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        dataSource = new HikariDataSource(config);

        // Запуск миграций Flyway перед всеми тестами
        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
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

        Draw saved = repository.save(draw);
        repository.updateStatus(saved.id(), status);
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
