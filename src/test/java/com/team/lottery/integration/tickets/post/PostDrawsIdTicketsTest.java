package com.team.lottery.integration.tickets.post;

import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.integration.BaseTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;

import static io.restassured.RestAssured.given;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.Matchers.*;

import static org.hamcrest.Matchers.anyOf;

import static org.hamcrest.Matchers.equalTo;



@Testcontainers
public class PostDrawsIdTicketsTest extends BaseTest {

    private String getUrl(int id) {
        return String.format("/draws/%d/tickets", id);
    }

    @Test
    public void buyTicketSuccessful() {
        /*
         * Сценарий: Пользователь покупает билет
         * 1. Админ создает тираж
         * 2. Юзер логинится
         * 3. Юзер покупает билет POST /draws/1/tickets
         * Ожидаемый результат: 200 OK, BuyTicketResponseV2 с ticket status=SOLD
         */

        // 1. Создание админа и тиража
        given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        given().header("Authorization", "Bearer " + adminToken).contentType(ContentType.JSON)
                .body("{ \"title\": \"Lottery 1\", \"totalTickets\": 100, \"endDate\": \"%s\" }".formatted(getDateInFuture()))
                .post("/admin/draws");

        // 2. Логин юзера
        given().contentType(ContentType.JSON).body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/register");
        String userToken = given().contentType(ContentType.JSON).body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/login").then().extract().path("token");

        // 3. Покупка билета
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post(getUrl(1))
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("ticket.status", equalTo("SOLD"))
                // Сначала регистрируем admin (он получает ID=1).
                // Затем регистрируем alice (она получает ID=2).
                .body("ticket.ownerId", equalTo(2)); // ID alice в чистой базе будет 2.
    }



    @Test
    public void buyTicketWithoutToken() {
        /* Попытка купить билет, не будучи авторизованным.
         * Сценарий: Отказ неавторизованному пользователю
         * Вход: POST /draws/1/tickets без токена
         * Ожидаемый результат: 401 Unauthorized
         */
        given()
                .contentType(ContentType.JSON)
                .when()
                .post(getUrl(1))
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("code", equalTo("UNAUTHORIZED"));
    }



    @Test
    public void buyTicketInNonExistentDraw() {
        /* Попутка купить билет несуществующего тиража.
         * Сценарий: Тираж не найден
         * 1. Регистрация и логин alice
         * 2. POST /draws/999/tickets с токеном alice
         * Ожидаемый результат: 404 Not Found
         */

        // 1. Регистрация и логин alice
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");

        String userToken = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then().extract().path("token");

        // 2. Попытка покупки в несуществующий тираж
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post(getUrl(999))
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    public void buySeveralTicketsByOneUser() {
        /*
        Покупка нескольких билетов одним пользователем
        Сценарий: Пользователь покупает 2 билета в разные тиражи
        Вход: POST /draws/1/tickets и POST /draws/2/tickets с Bearer-токеном alice
        Ожидаемый результат: 2 билета в /me/tickets
         */

        // 1. Создание админа и тиража
        given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String adminToken = given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login")
                .then().extract().path("token");

        // Создаем тираж и забираем его настоящий ID
        int drawId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"title\": \"Lottery 1\", \"totalTickets\": 100, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws")
                .then().extract().path("id");

        // 2. Регистрация и логин юзера
        given().contentType(ContentType.JSON).body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/register");
        String userToken = given().contentType(ContentType.JSON).body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/login")
                .then().extract().path("token");

        // 3. Покупка двух билетов в этот тираж
        for (int i = 0; i < 2; i++) {
            given()
                    .header("Authorization", "Bearer " + userToken)
                    .when()
                    .post(getUrl(drawId))
                    .then()
                    .statusCode(200)
                    .body("ticket.status", equalTo("SOLD"));
        }

        // 4. ФИНАЛЬНАЯ ПРОВЕРКА (согласно сценарию)
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/me/tickets")
                .then()
                .statusCode(200)
                .body("$", hasSize(2)) // Проверяем, что билетов именно 2
                .body("ownerId", everyItem(notNullValue()));
    }



    @Test
    public void buyTicketLimitExceeded() {
        /*
         * Сценарий: Попытка купить билет после исчерпания лимита
         * 1. Админ создает тираж с totalTickets = 1
         * 2. Юзер покупает первый (он же последний доступный) билет
         * 3. Юзер пытается купить второй билет в тот же тираж
         * Ожидаемый результат: 409 на второй попытке
         */

        // 1. Подготовка: Регистрация и получение токена админа
        given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        // 2. Создание тиража с лимитом в 1 билет
        int drawId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"title\": \"Limited Draw\", \"totalTickets\": 1, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws")
                .then()
                .statusCode(201)
                .extract().path("id");

        // 3. Подготовка юзера
        given().contentType(ContentType.JSON).body("{ \"login\": \"bob\", \"password\": \"password123\" }").post("/auth/register");
        String userToken = given().contentType(ContentType.JSON).body("{ \"login\": \"bob\", \"password\": \"password123\" }").post("/auth/login").then().extract().path("token");

        // 4. Покупка первого билета (успешно)
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post(getUrl(drawId))
                .then()
                .statusCode(200)
                .body("message", equalTo("Ticket purchased successfully"));

        // 5. Попытка покупки второго билета (ожидаем ошибку)
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post(getUrl(drawId))
                .then()
                .log().ifValidationFails()
                .statusCode(409) // Bad Request согласно спецификации для невозможной покупки
                .contentType(ContentType.JSON)
                .body("code", notNullValue())
                .body("message", anyOf(
                        containsString("No available tickets for this draw"),
                        containsString("sold out"),
                        containsString("Limit exceeded")
                ));
    }

}



