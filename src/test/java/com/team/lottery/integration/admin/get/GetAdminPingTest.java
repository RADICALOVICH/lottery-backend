package com.team.lottery.integration.admin.get;

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
public class GetAdminPingTest extends BaseTest {

    private String url = "/admin/ping";

    @Test
    public void adminPingSuccessful() {
        /* Пинг админа (успех)
         * Сценарий: Проверка ADMIN доступа
         * 1. Регистрация и повышение прав до ADMIN
         * 2. Авторизация админа для получения токена
         * 3. Запрос /admin/ping с токеном админа
         */

        // 1. Регистрация
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        // 2. Повышение прав в БД
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            var statement = connection.prepareStatement("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update role", e);
        }

        // 3. Логин и получение токена
        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 4. Запрос к административному эндпоинту
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Admin access granted"))
                .body("login", equalTo("admin"))
                .body("role", equalTo("ADMIN"));
    }

    @Test
    public void adminPingWithoutToken() {
        /* Пинг админа без токена
         * Сценарий: Отказ без авторизации
         * Вход: GET /admin/ping без токена
         * Ожидаемый результат: 401 Unauthorized
         */
        given()
                .when()
                .get(url)
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    public void adminPingWhenNotLoggedIn() {
        /* Попытка проверить административный доступ, не выполнив вход.
         * Сценарий: Отказ USER'у в админском доступе
         * 3. Запрос /admin/ping без токена
         * Ожидаемый результат: 401
         */
        given()
                .when()
                .get(url)
                .then()
                .statusCode(org.hamcrest.Matchers.anyOf(equalTo(401)));
    }

    @Test
    public void adminPingByUser() {
        /* Попытка проверить административный доступ без административных прав (пинг USER'ом).
         * Сценарий: Отказ USER'у в админском доступе
         * 1. Регистрация пользователя alice
         * 2. Авторизация alice
         * 3. Запрос /admin/ping с токеном alice
         * Ожидаемый результат: 403 Forbidden
         */

        // 1. Регистрация
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");

        // 2. Логин и получение токена
        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 3. Запрос к административному эндпоинту
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(url)
                .then()
                .statusCode(org.hamcrest.Matchers.anyOf(equalTo(403)));
    }



}



