package com.team.lottery.integration.draws.get;

import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.integration.BaseTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;


@Testcontainers
public class GetDrawsTest extends BaseTest {
    private String url = "/draws";

    @Test
    public void getAllDraws() {
        /* Список тиражей (все статусы)
         * Сценарий: Получение всех тиражей без фильтра
         * 1. Подготовка: создаем админа и авторизуемся
         * 2. Создаем один тираж
         * 3. GET /draws
         * Ожидаемый результат: 200 OK, массив с 1 тиражом status=ACTIVE
         */

        // 1. Создаем админа и получаем токен (для создания тиража)
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
                          "title": "Тираж #1",
                          "totalTickets": 1000,
                          "endDate": "%s"
                        }
                        """, getDateInFuture()))
                .post("/admin/draws");

        // 3. Получаем список всех тиражей
        given()
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(1))
                .body("[0].title", equalTo("Тираж #1"))
                .body("[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void filterDrawsByStatus() {

        /*
         * Сценарий: Фильтрация ACTIVE тиражей
         * 1. Подготовка: создаем админа, получаем токен
         * 2. Создаем один тираж (он по умолчанию ACTIVE)
         * 3. GET /draws?status=ACTIVE
         * Ожидаемый результат: Массив с тиражом.
         */

        // 1. Создание админа и логин
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
                          "title": "Активный тираж",
                          "totalTickets": 500,
                          "endDate": "%s"
                        }
                        """, getDateInFuture()))
                .post("/admin/draws");

        // 3. Выполняем GET запрос с фильтром
        given()
                .queryParam("status", "ACTIVE")
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(1))
                .body("[0].title", equalTo("Активный тираж"))
                .body("[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void getDrawByNonNumericIdReturns400() {
        /*
         * Сценарий: GET /draws/abc — id должен быть числом.
         * Ожидаемый результат: 400 VALIDATION_FAILED со ссылкой на параметр id
         *                      и принятым значением.
         */
        given()
                .when()
                .get("/draws/abc")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("message", containsString("id"))
                .body("message", containsString("abc"));
    }

    @Test
    public void filterDrawsByInvalidStatus() {
        /*
         * Сценарий: GET /draws?status=CANCELED — статуса CANCELED нет в enum.
         * Ожидаемый результат: 400 VALIDATION_FAILED со списком допустимых
         *                      значений (ACTIVE, CLOSED, COMPLETED) и принятым
         *                      значением в сообщении.
         */
        given()
                .queryParam("status", "CANCELED")
                .when()
                .get(url)
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("message", containsString("ACTIVE"))
                .body("message", containsString("CLOSED"))
                .body("message", containsString("COMPLETED"))
                .body("message", containsString("CANCELED"));
    }

}



