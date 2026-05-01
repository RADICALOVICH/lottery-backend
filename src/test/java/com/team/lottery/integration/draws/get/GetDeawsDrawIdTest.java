package com.team.lottery.integration.draws.get;

import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.integration.BaseTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.Matchers.anyOf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;


@Testcontainers
public class GetDeawsDrawIdTest extends BaseTest {

    private String getUrl(int id) {
        return String.format("/draws/%s", id);
    }

    @Test
    public void getDrawById() {
        /*
         * Сценарий: Получение конкретного тиража
         * 1. Подготовка: создаем админа, логинимся, создаем тираж
         * 2. GET /draws/1
         * Ожидаемый результат: 200 OK, DrawDto ID=1
         */

        // 1. Создаем админа и авторизуемся
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set admin role", e);
        }

        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 2. Создаем тираж
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "title": "Тираж для теста ID",
                          "totalTickets": 200,
                          "endDate": "%s"
                        }
                        """, getDateInFuture()))
                .post("/admin/draws");

        // 3. Запрашиваем тираж по ID=1
        given()
                .when()
                .get(getUrl(1))
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("title", equalTo("Тираж для теста ID"))
                .body("totalTickets", equalTo(200))
                .body("status", equalTo("ACTIVE"));
    }


    @Test
    public void getNonExistentDraw() {
        /*
         * Сценарий: Поиск несуществующего тиража
         * Вход: GET /draws/999
         * Ожидаемый результат: 404 Not Found
         */
        given()
                .when()
                .get(getUrl(999))
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", equalTo("NOT_FOUND"));
    }



}



