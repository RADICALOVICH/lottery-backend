package com.team.lottery.integration.draws.post;

import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.integration.BaseTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.Matchers.*;

import static org.hamcrest.Matchers.anyOf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;


@Testcontainers
public class PostAdminDrawsTest extends BaseTest {

    private String url = "/admin/draws";

    @Test
    void shouldReturn409WhenEndDateInPast() {
        /*
        Дата окончания тиража в прошлом
        Сценарий: Создание тиража с endDate < now()
        Вход: POST /admin/draws с endDate в прошлом
        Ожидаемый результат: 409
        * */

        // 1. Создание админа (используем вспомогательный подход)
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set admin role", e);
        }

        // 2. Логин
        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 3. Создание тиража. Дата - в прошлом.
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Тираж #1",
                          "totalTickets": 1000,
                          "endDate": "2024-04-25T18:00:00Z"
                        }
                        """)
                .when()
                .post(url)
                .then()
                .statusCode(409)
                .body("code", equalTo("CONFLICT"))
                .body("message", anyOf(
                        containsString("The draw end date cannot be in the past")
                ));
    }

}



