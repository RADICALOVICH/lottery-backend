package com.team.lottery.integration.authentication.post;

import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.integration.BaseTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;

import static io.restassured.RestAssured.given;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.Matchers.anyOf;

import static org.hamcrest.Matchers.equalTo;



@Testcontainers
public class PostAuthLoginTest extends BaseTest {
    private String url = "/auth/login";
    @Test
    public void loginSuccessful() {
        /* Логин успешный.
         * Сценарий: Авторизация существующего пользователя
         * 1. Регистрируем пользователя
         * 2. Выполняем вход
         * Ожидаемый результат: 200 OK
         */

        // Шаг 1: Создаем пользователя
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "login": "alice",
                          "password": "supersecret123"
                        }
                        """)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        // Шаг 2: Выполняем логин
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
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Login successful"))
                .body("token", org.hamcrest.Matchers.notNullValue()) // Токен должен быть сгенерирован
                .body("id", equalTo(1))
                .body("login", equalTo("alice"))
                .body("role", equalTo("USER"));
    }

    @Test
    public void loginWithWrongPassword() {
        /*
        Логин с неверным паролем
        Сценарий: Отказ при неверных учетных данных
        Вход: POST /auth/login с { "login": "alice", "password": "wrong" }
        Ожидаемый результат: 401 Unauthorized, { "code": "UNAUTHORIZED", "message": "Invalid credentials" }
         */

        // 1. Регистрируем пользователя с валидным паролем (например, "pass12345678")
        given()
                .contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"pass12345678\" }")
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        // 2. Пытаемся войти с валидным по формату, но неверным паролем ("wrongpass123")
        given()
                .contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"wrongpass123\" }")
                .when()
                .post(url)
                .then()
                .statusCode(401) // Теперь пройдет валидацию и упадет на проверке хеша -> 401
                .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    public void loginNonExistentUser() {
        /* Логин несуществующего пользователя.
         * Сценарий: Отказ при несуществующем логине.
         * Вход: POST /auth/login с { "login": "bob", "password": "pass123" }
         * Ожидаемый результат: 401 Unauthorized
         */
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "login": "nonexistent_user",
                          "password": "pass1234"
                        }
                        """)
                .when()
                .post(url)
                .then()
                .statusCode(401) // Теперь валидатор пропустит запрос дальше
                .body("code", equalTo("UNAUTHORIZED"));
    }


    @Test
    public void loginAdminSuccessful() {
        /* Логин админа
         * Сценарий: Авторизация администратора
         * 1. Регистрация пользователя "admin"
         * 2. Повышение его прав до ADMIN
         * 3. Авторизация и проверка роли
         * Ожидаемый результат: 200 OK с role: "ADMIN"
         */

        // 1. Регистрация
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register")
                .then()
                .statusCode(201);

        // 2. Прямое повышение прав в БД
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            var statement = connection.prepareStatement("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user role to ADMIN", e);
        }

        // 3. Логин и проверка роли
        given()
                .contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .when()
                .post(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("login", equalTo("admin"))
                .body("role", equalTo("ADMIN"));
    }

        @Test
    public void loginAfterLogout() {
        /*
        Повторный логин после логаута
        Сценарий: Получение нового токена
        Вход: Логин → логаут → повторный логин alice
        Ожидаемый результат: Новый Bearer-токен успешно получен
        и работает.
        * */
        // 1. Регистрация alice
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");


        // 2. Авторизация
        String userToken = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post(url)
                .then()
                .extract()
                .path("token");


        // 3. Разлогиниваемся
        given()
                .header("Authorization", "Bearer " + userToken)
                .post("/auth/logout")
                .then()
                .statusCode(200);

        // 4. Логинимся снова
        String newUserToken = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post(url)
                .then()
                .extract()
                .path("token");

        // 5. Проверяем, что новый токен работает
        given()
                .header("Authorization", "Bearer " + newUserToken)
                .get("/users/me")
                .then()
                .statusCode(200)
                .body("login", equalTo("alice"));
    }


}



