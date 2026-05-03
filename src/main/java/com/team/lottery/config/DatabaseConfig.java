package com.team.lottery.config;

//какая-то не понятная штука, надо будет разобраться..

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Настройка доступа к БД:
 *   1. Поднимает HikariCP-пул на основе AppConfig.
 *   2. Прогоняет миграции Flyway из classpath:db/migration.
 *   3. Предоставляет доступ к DataSource для тестов.
 */
public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    // Static reference to hold the instance for access across the app and tests
    private static HikariDataSource dataSource;

    private DatabaseConfig() {
    }

    public static DataSource init(AppConfig cfg) {
        if (dataSource != null) {
            return dataSource;
        }

        dataSource = createDataSource(cfg);
        runMigrations(dataSource);
        return dataSource;
    }

    public static HikariDataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("Database not initialized. Call init(AppConfig) first.");
        }
        return dataSource;
    }

    private static HikariDataSource createDataSource(AppConfig cfg) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(cfg.dbUrl());
        hc.setUsername(cfg.dbUser());
        hc.setPassword(cfg.dbPassword());
        hc.setMaximumPoolSize(cfg.dbPoolSize());
        hc.setPoolName("lottery-pool");

        HikariDataSource ds = new HikariDataSource(hc);
        log.info("Hikari pool started: url={}, poolSize={}", cfg.dbUrl(), cfg.dbPoolSize());
        return ds;
    }

    private static void runMigrations(DataSource ds) {
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();
        log.info(
                "Flyway migrations applied: count={}, targetSchemaVersion={}",
                result.migrationsExecuted,
                result.targetSchemaVersion
        );
    }
}