package com.team.lottery.integration.draws.post;

import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.integration.BaseTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;

import static io.restassured.RestAssured.given;


import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.Matchers.anyOf;

import static org.hamcrest.Matchers.equalTo;



@Testcontainers
public class PostAdminDrawsIdRunDraw extends BaseTest {
    public String getUrl(int id) {
        return String.format("/admin/draws/%d/run-draw", id);
    }




    @Test
    public void runDrawAsAdmin() {
        /* Проведение розыгрыша (админ)
         * Сценарий: Запуск розыгрыша тиража
         * Вход: POST /admin/draws/1/run-draw с JWT админа
         * Ожидаемый результат: 200 OK, DrawResponse status=COMPLETED
         * */
        // Регистрация парсера для обработки text/plain как текста
        RestAssured.registerParser("text/plain", Parser.TEXT);

        // 1. Создаем админа, создаем тираж
        given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        given().header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .body(String.format("{ \"title\": \"Тираж\", \"totalTickets\": 10, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws");

        // 2. Покупка билета
        given().contentType("application/json").body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/register");
        String userToken = given().contentType("application/json").body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/login").then().extract().path("token");
        given().header("Authorization", "Bearer " + userToken).post("/draws/1/tickets");

        // 3. Ставим статус CLOSED
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE draws SET status = 'CLOSED' WHERE id = 1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 4. Выполнение
        Response response = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .post(getUrl(1));

        // 5. Проверка:
        response.then()
                .statusCode(200)
                // Проверяем, что Content-Type именно JSON (если тело пустое, это обычно не так)
                .contentType(ContentType.JSON)
                // Проверяем значение в теле
                .body("status", equalTo("COMPLETED"));
    }

}



