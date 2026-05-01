package com.team.lottery.integration.admin.post;

import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.integration.BaseTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.anyOf;

import static org.hamcrest.Matchers.equalTo;



@Testcontainers
public class PostAdminDrawsTest extends BaseTest {

    private String url = "/admin/draws";

    @Test
    public void createASecondDrawAsAdmin() {
        /*
            Создание второго тиража
            Сценарий: Множественные тиражи
            Вход: POST /admin/draws с { "title": "Тираж #2"... }
            Ожидаемый результат: 201, Тираж #2"
         */

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

        // 3. Создание тиража
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "title": "Тираж #1",
                          "totalTickets": 1000,
                          "endDate": \"%s\"
                        }
                        """, getDateInFuture()))
                .when()
                .post("/admin/draws")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("title", equalTo("Тираж #1"))
                .body("totalTickets", equalTo(1000))
                .body("status", equalTo("ACTIVE"));

        // 4. Создание второго тиража
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "title": "Тираж #2",
                          "totalTickets": 2000,
                          "endDate": \"%s\"
                        }
                        """, getDateInFuture()))
                .when()
                .post(url)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo(2))
                .body("title", equalTo("Тираж #2"))
                .body("totalTickets", equalTo(2000))
                .body("status", equalTo("ACTIVE"));
    }






    @Test
    public void createDrawAsAdmin() {
        /*
         * Сценарий: Создание нового тиража
         * 1. Регистрация и повышение прав admin
         * 2. Авторизация admin
         * 3. POST /admin/draws с данными тиража
         * Ожидаемый результат: 201 Created, DrawResponse с ID=1, status=ACTIVE
         */

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

        // 3. Создание тиража
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "title": "Тираж #1",
                      "totalTickets": 1000,
                      "endDate": "%s"
                    }
                    """.formatted(getDateInFuture()))
                .when()
                .post(url)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("title", equalTo("Тираж #1"))
                .body("totalTickets", equalTo(1000))
                .body("status", equalTo("ACTIVE"));
    }



    @Test
    public void createDrawWithEmptyTitle() {
        /* Валидация параметров тиража
         * Сценарий: Создадим тираж с пустым title.
         * 1. Подготовка: регистрация админа и получение токена
         * 2. POST /admin/draws без title
         * Ожидаемый результат: 400 Bad Request
         */

        // 1. Регистрация и получение токена админа
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

        // 2. Попытка создания тиража с пустым title
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "totalTickets": 100,
                          "endDate": "%s"
                        }
                        """.formatted(getDateInFuture()))
                .when()
                .post(url)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("VALIDATION_FAILED"));
    }

}



