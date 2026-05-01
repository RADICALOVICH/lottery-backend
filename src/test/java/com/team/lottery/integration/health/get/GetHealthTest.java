package com.team.lottery.integration.health.get;

import com.team.lottery.integration.BaseTest;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;

import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.Matchers.equalTo;


import static org.hamcrest.Matchers.anyOf;

import static io.restassured.RestAssured.given;


@Testcontainers
public class GetHealthTest extends BaseTest {
    private String url = "/health";
    @Test
    public void healthStatusUp() {
        /*
        Health-check базовый
        Сценарий: Проверка доступности сервиса
        Вход: GET /health
        Ожидаемый результат: 200 OK, { "status": "UP", "service": "lottery-api" }
        * */
        given()
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", equalTo("UP"))
                .body("service", equalTo("lottery-api"));
    }

}



