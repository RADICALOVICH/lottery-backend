package com.team.lottery.integration.authentication.post;

import com.team.lottery.integration.BaseTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;


import static org.hamcrest.Matchers.anyOf;

import static io.restassured.RestAssured.given;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.Matchers.equalTo;


@Testcontainers
public class PostAuthLogoutTest extends BaseTest {
    private String url = "/auth/logout";
    @Test
    public void logoutSuccessful() {
        /* Выход из системы
         * Сценарий: Успешный логаут
         * 1. Регистрируем alice
         * 2. Логинимся для получения токена
         * 3. Выполняем logout с этим токеном
         * Ожидаемый результат: 200 Logout successful.
         */

        // 1. Регистрация
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");

        // 2. Логин для получения токена
        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 3. Выполнение logout
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Logout successful"));
    }



    @Test
    public void logoutSuccess() {
        /*
         * Сценарий: проверка выхода.
         * 1. Регистрация alice
         * 2. Авторизация alice
         * 3. Запрос POST /auth/logout
         * Ожидаемый результат: 200 Ok.  */

        // 1. Регистрация alice
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");


        // 2. Авторизация
        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post(url)
                .then()
                .statusCode(200);
    }

    @Test
    public void logoutWithoutAuth() {
        /*
         * Нельзя разлогинить того, кто не вошел
         * Ожидаемый результат: 401.
         * */
        given()
                .when()
                .post(url)
                .then()
                .statusCode(401);
    }

    @Test
    public void tokenInvalidationAfterLogout() {
        /*
        Логаут и сразу попытка доступа
        Сценарий: Проверка инвалидации токена
        Вход: Логин alice → /users/me → логаут → /users/me с тем же токеном
        Ожидаемый результат: 401 после логаута
        */

        // 1. Регистрация alice
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");


        // 2. Авторизация
        String userToken = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        /* 3. Проверка инвалидации токена: Logout и сразу попытка доступа */
        // Сначала проверяем, что доступ есть
        given()
                .header("Authorization", "Bearer " + userToken)
                .get("/users/me")
                .then()
                .statusCode(200);
        // Выполняем логаут
        given()
                .header("Authorization", "Bearer " + userToken)
                .post(url)
                .then()
                .statusCode(200);
        // Пытаемся зайти с тем же токеном снова
        given()
                .header("Authorization", "Bearer " + userToken)
                .get("/users/me")
                .then()
                .statusCode(401); // Ожидаем отказ, так как токен должен быть недействителен
    }

}



