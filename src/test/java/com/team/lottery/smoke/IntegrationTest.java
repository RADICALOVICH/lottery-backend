package com.team.lottery.smoke;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers
public class IntegrationTest {

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
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