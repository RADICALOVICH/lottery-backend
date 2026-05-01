package com.team.lottery.integration.users.get;

import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.integration.BaseTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static net.bytebuddy.matcher.ElementMatchers.anyOf;
import static org.hamcrest.Matchers.equalTo;


import static io.restassured.RestAssured.given;


import io.restassured.response.Response;

import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.Matchers.anyOf;

@Testcontainers
public class GetMeResultsTest extends BaseTest {

    String url = "/me/results";


    @Test
    public void getMyResultsBeforeDraw() {
        /* Попытка пользователя увидеть свои результаты до розыгрыша.
         * Сценарий: Результаты до проведения тиража
         * 1. Подготовка: регистрация alice, создание тиража админом, покупка билета
         * 2. GET /me/results с токеном alice
         * Ожидаемый результат: 200 OK, пустой массив
         */

        // 1. Подготовка: Регистрация alice и логин
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");
        String userToken = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then().extract().path("token");

        // 2. Подготовка: Создание тиража админом
        given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        given().header("Authorization", "Bearer " + adminToken).contentType(ContentType.JSON)
                .body(String.format("{ \"title\": \"Тираж #1\", \"totalTickets\": 100, \"endDate\": %s }", "2027-01-01T00:00:00Z"))
                .post("/admin/draws");

        // 3. Покупка билета alice
        given().header("Authorization", "Bearer " + userToken).post("/draws/1/tickets");

        // 4. Проверка результатов (ожидаем пустоту)
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(0)); // Пустой массив
    }


    @Test
    public void getUserResults() {
        /* Мои результаты после розыгрыша
         * Сценарий: Проверка статуса билетов пользователя после розыгрыша
         * 1. Подготовка: Создаем тираж, покупаем билет для Alice
         * 2. Розыгрыш: Закрываем тираж и запускаем run-draw
         * 3. GET /me/results: Проверяем, что билеты имеют статус WIN или LOSE
         * Ожидаемый результат: Билет со статусом WIN или LOSE
         */

        // 1. Подготовка: Регистрация Alice и получение токена
        given().contentType("application/json")
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");
        String aliceToken = given().contentType("application/json")
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then().extract().path("token");

        // Регистрация админа и создание тиража
        given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        given().header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .body(String.format("{ \"title\": \"Тираж для результатов\", \"totalTickets\": 10, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws");

        // Alice покупает билет
        given().header("Authorization", "Bearer " + aliceToken).post("/draws/1/tickets");

        // 2. Проведение розыгрыша (перевод статусов билетов из SOLD в WIN/LOSE)
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE draws SET status = 'CLOSED' WHERE id = 1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given().header("Authorization", "Bearer " + adminToken).post("/admin/draws/1/run-draw");

        // 3. Запрос результатов пользователя
        Response response = given()
                .header("Authorization", "Bearer " + aliceToken)
                .when()
                .get(url);

        // 4. Проверка: статус должен быть либо WIN, либо LOSE
        response.then()
                .statusCode(200)
                .body("[0].status", anyOf(equalTo("WIN"), equalTo("LOSE")));
    }

}



