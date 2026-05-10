package com.team.lottery.integration.users.get;

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
public class GetUsersMeTest extends BaseTest {

    private String url = "/users/me";

    @Test
    public void getMeSuccessful() {
        /* Инфо о текущем пользователе
         * Сценарий: Получение данных авторизованного пользователя
         * 1. Регистрируем alice
         * 2. Логинимся, чтобы получить токен
         * 3. Запрашиваем /users/me с токеном
         *    GET /users/me с Bearer-токеном alice
         * Ожидаемый результат: 200 OK, { "id": 1, "login": "alice", "role": "USER" }
         */

        // 1. Регистрация
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice111\", \"password\": \"supersecret123\" }")
                .post("/auth/register");

        // 2. Логин и извлечение токена
        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice111\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 3. Запрос профиля с токеном
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("login", equalTo("alice111"))
                .body("role", equalTo("USER"));
    }

    @Test
    public void getMeWithoutToken() {
        /* Инфо о пользователе без токена
         * Сценарий: Доступ без авторизации
         * Вход: GET /users/me без токена
         * Ожидаемый результат: 401 Unauthorized
         */
        given()
                .when()
                .get("/users/me")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("code", equalTo("UNAUTHORIZED"));
    }

}



