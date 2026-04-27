package com.team.lottery.smoke;

import com.team.lottery.Application;
import com.team.lottery.config.AppConfig;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers
public class IntegrationTest {

    private Javalin app;
    private int port;

    @BeforeEach
    public void setUp() {
        AppConfig cfg = AppConfig.load();
        port = cfg.port();  // ← порт из application.properties

        app = Application.start(port);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    public void healthStatusUp() {

        /*
         * Сценарий: Проверка доступности сервиса
         * Вход: GET /health
         * Ожидаемый результат: 200 OK, { "status": "UP", "service": "lottery-api" }
         * */

        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", equalTo("UP"))
                .body("service", equalTo("lottery-api"));
    }
}