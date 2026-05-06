package com.team.lottery.integration;

import com.team.lottery.Application;
import com.team.lottery.config.AppConfig;
import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.support.TestPostgres;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public abstract class BaseTest {

    private static Javalin app;
    private static final int TEST_PORT = 8082; // Используем отдельный порт для тестов


    public String getDateInFuture() {
        // Получить дату в будущем.
        // Дата проведения тиража должна быть в будущем.

        return OffsetDateTime.now(ZoneOffset.UTC)
                .plusYears(1)
                .toInstant()
                .toString();
    }


    @BeforeAll
    public static void startApp() {
        // Собираем AppConfig для тестов: порт + JDBC из изолированного
        // Testcontainer'а, остальное — дефолты из application.properties.
        AppConfig defaults = AppConfig.load();
        AppConfig testCfg = new AppConfig(
                TEST_PORT,
                TestPostgres.INSTANCE.getJdbcUrl(),
                TestPostgres.INSTANCE.getUsername(),
                TestPostgres.INSTANCE.getPassword(),
                defaults.dbPoolSize(),
                defaults.bcryptCost(),
                defaults.drawSchedulerIntervalSeconds(),
                defaults.isProd()
        );

        app = Application.startWith(testCfg);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = TEST_PORT;

        // Регистрируем парсер один раз для всего класса тестов
        RestAssured.registerParser("text/plain", Parser.TEXT);
    }

    @BeforeEach
    public void cleanDatabase() {
        // Очищаем базу перед каждым тестом, чтобы обеспечить изоляцию.
        // DatabaseConfig.getDataSource() возвращает datasource, привязанный к
        // изолированному Testcontainer'у (см. TestPostgres + Application.startWith).
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("TRUNCATE TABLE users, draws, tickets, draw_results RESTART IDENTITY CASCADE");
        } catch (Exception e) {
            throw new RuntimeException("Failed to truncate database during setup", e);
        }
    }

    @AfterAll
    public static void stopApp() {
        if (app != null) {
            app.stop();
        }
    }
}
