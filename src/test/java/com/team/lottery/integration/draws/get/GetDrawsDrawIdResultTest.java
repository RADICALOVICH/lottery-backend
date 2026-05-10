package com.team.lottery.integration.draws.get;

import com.team.lottery.integration.BaseTest;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.hamcrest.Matchers.notNullValue;

import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.anyOf;


import com.team.lottery.config.DatabaseConfig;


import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.equalTo;

@Testcontainers
public class GetDrawsDrawIdResultTest extends BaseTest {

    private String getUrl(int id) {
        return String.format("/draws/%d/result", id);
    }

    @Test
    public void getDrawResult() {
        /* Результат розыгрыша
         * Сценарий: Получение результата розыгрыша
         * 1. Подготовка: создаем тираж, покупаем билет, проводим розыгрыш
         * 2. Отладка: проверяем наличие записи в БД перед запросом
         * 3. GET /draws/1/result
         * 4. Проверка: наличие данных о победителе
         * Ожидаемый результат: 200 OK
         */

        // 1. Подготовка: регистрация админа, токен, создание тиража
        given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        given().header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .body(String.format("{ \"title\": \"Тираж для результата\", \"totalTickets\": 10, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws");

        // Покупка билета
        given().contentType("application/json").body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/register");
        String userToken = given().contentType("application/json").body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/login").then().extract().path("token");
        given().header("Authorization", "Bearer " + userToken).post("/draws/1/tickets");

        // Принудительное закрытие и проведение розыгрыша
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE draws SET status = 'CLOSED' WHERE id = 1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given().header("Authorization", "Bearer " + adminToken).post("/admin/draws/1/run-draw");

        // 2. GET запрос на получение результата
        Response response = given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(getUrl(1));

        // 3. Проверка результата
        response.then()
                .statusCode(200)
                .body("drawId", equalTo(1))
                .body("winningTicketId", notNullValue());
    }

}



