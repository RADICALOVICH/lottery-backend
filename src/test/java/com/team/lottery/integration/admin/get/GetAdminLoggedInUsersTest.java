package com.team.lottery.integration.admin.get;

import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.integration.BaseTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


import static org.hamcrest.Matchers.notNullValue;

import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.anyOf;

import static org.hamcrest.Matchers.equalTo;



@Testcontainers
public class GetAdminLoggedInUsersTest extends BaseTest {
    private String url = "/admin/logged-in-users";

    @Test
    public void testGetLoggedInUsersAsAdmin() {

        /* Активные пользователи (запрос от администратора)
          Сценарий: Список залогиненных пользователей
          Вход: GET /admin/logged-in-users с JWT админа (после логина alice и admin)
          Ожидаемый результат: Массив с активными сессиями
        */


        // 1. Регистрация обычного пользователя
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "alice", "password", "password123"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        // 2. Регистрация будущего админа
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "admin", "password", "admin-pass"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 4. Логин Alice (создаем сессию)
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "alice", "password", "password123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200);

        // 5. Логин Admin (получаем токен с правами ADMIN)
        String adminToken = given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "admin", "password", "admin-pass"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");

        // 6. Финальная проверка списка активных сессий
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get(url)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("login", hasItems("alice", "admin"));
    }

    @Test
    public void testGetLoggedInUsersAsUser() {

        /* Активные пользователи (запрос от пользователя)
          Сценарий: Список залогиненных пользователей
          Вход: GET /admin/logged-in-users с JWT пользователя (после логина alice и admin)
          Ожидаемый результат: Массив с активными сессиями
        */


        // 1. Регистрация обычного пользователя
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "alice", "password", "password123"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        // 2. Регистрация будущего админа
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "admin", "password", "admin-pass"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 4. Логин Alice (создаем сессию)
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "alice", "password", "password123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200);

        // 5. Логин Admin (получаем токен с правами ADMIN)
        String userToken = given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "alice", "password", "password123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");

        // 6. Финальная проверка списка активных сессий
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(url)
                .then()
                .log().ifValidationFails()
                .statusCode(403).
                contentType(ContentType.JSON)
                .body("code", equalTo("FORBIDDEN"));
    }

    @Test
    public void testGetLoggedInUsersWhenNotLoggedIn() {

        /* Активные пользователи (запрос от пользователя)
          Сценарий: Список залогиненных пользователей
          Вход: GET /admin/logged-in-users без JWT пользователя (после логина alice и admin)
          Ожидаемый результат: 401
        */


        // 1. Регистрация обычного пользователя
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "alice", "password", "password123"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        // 2. Регистрация будущего админа
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "admin", "password", "admin-pass"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 4. Логин Alice (создаем сессию)
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "alice", "password", "password123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200);

        // 5. Логин Admin (получаем токен с правами ADMIN)
        String userToken = given()
                .contentType(ContentType.JSON)
                .body(Map.of("login", "alice", "password", "password123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");

        // 6. Финальная проверка списка активных сессий
        given()
                .when()
                .get(url)
                .then()
                .log().ifValidationFails()
                .statusCode(401).
                contentType(ContentType.JSON)
                .body("code", equalTo("UNAUTHORIZED"));
    }


}



