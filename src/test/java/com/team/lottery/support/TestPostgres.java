package com.team.lottery.support;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton-контейнер PostgreSQL для всех тестов проекта.
 *
 * Запускается один раз при первом обращении к INSTANCE, миграции прогоняются
 * сразу. Все тест-классы (BaseTest для integration, BaseJdbcDrawRepositoryTest
 * для repo-тестов) используют этот же контейнер — один контейнер на весь
 * `./gradlew test`.
 *
 * Lifecycle контейнера управляется Testcontainers' Ryuk — контейнер
 * автоматически прибивается при выходе тестового JVM.
 */
public final class TestPostgres {

    public static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("lottery_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        INSTANCE.start();
        Flyway.configure()
                .dataSource(INSTANCE.getJdbcUrl(), INSTANCE.getUsername(), INSTANCE.getPassword())
                .load()
                .migrate();
    }

    private TestPostgres() {
    }
}
