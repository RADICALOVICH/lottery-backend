package com.team.lottery.config;

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
 *
 * Падение на любом этапе — IllegalStateException: приложение не должно стартовать без БД.
 */
public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private DatabaseConfig() {
    }

    public static DataSource init(AppConfig cfg) {
        HikariDataSource ds = createDataSource(cfg);
        runMigrations(ds);
        return ds;
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
