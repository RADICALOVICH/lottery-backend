package com.team.lottery.integration.authentication.post;

import com.team.lottery.integration.BaseTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.Matchers.anyOf;

import static org.hamcrest.Matchers.equalTo;


import static io.restassured.RestAssured.given;


@Testcontainers
public class PostAuthRegisterTest extends BaseTest {

    private String url = "/auth/register";

    @Test
    public void registerNewUser() {
        /*
        Регистрация успешная
        Сценарий: Создание нового пользователя
        Вход: POST /auth/register с { "login": "alice", "password": "supersecret123" }
        Ожидаемый результат: 201 Created, { "id": 1, "login": "alice", "message": "User registered successfully" }
         */

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "login": "alice",
                          "password": "supersecret123"
                        }
                        """)
                .when()
                .post(url)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("login", equalTo("alice"))
                .body("message", equalTo("User registered successfully"));
    }


    @Test
    public void registerWithEmptyLogin() {
        /*
         * Регистрация с пустым логином
         * Сценарий: Валидация обязательных полей
         * Вход: POST /auth/register с { "password": "pass123" }
         * Ожидаемый результат: 400 Bad Request, { "code": "VALIDATION_FAILED", "message": "Login is required" }
         */
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "password": "pass123"
                        }
                        """)
                .when()
                .post(url)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("message", equalTo("Login is required"));
    }


    @Test
    public void registerWithShortLogin() {
        /* Регистрация с коротким логином
         * Сценарий: Проверка минимальной длины логина
         * Вход: POST /auth/register с { "login": "ab", "password": "pass123" }
         * Ожидаемый результат: 400 Bad Request
         */
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "login": "ab",
                          "password": "pass123"
                        }
                        """)
                .when()
                .post(url)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("VALIDATION_FAILED"));
    }

    @Test
    public void registerWithShortPassword() {
        /* Регистрация с коротким паролем
         * Сценарий: Проверка минимальной длины пароля
         * Вход: POST /auth/register с { "login": "alice2", "password": "123" }
         * Ожидаемый результат: 400 Bad Request, код валидации
         */
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "login": "alice2",
                          "password": "123"
                        }
                        """)
                .when()
                .post(url)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("VALIDATION_FAILED"));
    }


    @Test
    public void registerDuplicateLogin() {
        /* Регистрация дублирующего логина.
         * Сценарий: Проверка уникальности логина.
         * 1. Создаем пользователя "alice"
         * 2. Пытаемся создать его снова
         * Ожидаемый результат: 409 Conflict
         */

        // Шаг 1: Успешная регистрация первого пользователя
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "login": "alice",
                          "password": "supersecret123"
                        }
                        """)
                .when()
                .post(url)
                .then()
                .statusCode(201);

        // Шаг 2: Попытка регистрации дубликата
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "login": "alice",
                          "password": "pass1234"
                        }
                        """)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(409)
                .contentType(ContentType.JSON)
                .body("code", equalTo("CONFLICT"))
                .body("message", equalTo("Login already exists"));
    }

}



