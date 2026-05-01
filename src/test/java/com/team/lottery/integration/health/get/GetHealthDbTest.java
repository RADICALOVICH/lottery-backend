package com.team.lottery.integration.health.get;

import com.team.lottery.integration.BaseTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;


import io.restassured.http.ContentType;

import static org.hamcrest.Matchers.notNullValue;

import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.equalTo;


import static io.restassured.RestAssured.given;


import static org.hamcrest.Matchers.anyOf;


@Testcontainers
public class GetHealthDbTest extends BaseTest {
    private String url = "/health/db";
    @Test
    public void healthDbStatusUp() {
        /*
        Сценарий: Проверка подключения к БД
        Вход: GET /health/db
        Ожидаемый результат: 200 OK, { "status": "UP", "database": "CONNECTED", "dbName": "lottery", "dbUser": "postgres" }
        * */
        given()
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", equalTo("UP"))
                .body("database", equalTo("CONNECTED"))
                .body("dbName", equalTo("lottery"))
                .body("dbUser", equalTo("lottery"));
    }

}



