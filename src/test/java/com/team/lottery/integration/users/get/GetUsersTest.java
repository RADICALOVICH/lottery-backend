package com.team.lottery.integration.users.get;

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
public class GetUsersTest extends BaseTest {

    private String url = "/users";

    @Test
    public void getAllUsersByAdmin() {
        /*
         * Сценарий: Получение списка пользователей админом
         * 1. Регистрация alice
         * 2. Регистрация admin и повышение его прав
         * 3. Авторизация admin
         * 4. Запрос GET /users
         * Ожидаемый результат: 200 OK, массив с alice и admin
         */

        // 1. Регистрация alice
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");

        // 2. Регистрация и повышение прав admin
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set admin role", e);
        }

        // 3. Авторизация admin
        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 4. Запрос списка пользователей
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                // Проверяем, что массив содержит именно двух пользователей
                .body("size()", equalTo(2))
                .body("login", org.hamcrest.Matchers.containsInAnyOrder("alice", "admin"));
    }


    @Test
    public void getAllUsersByOrdinaryUser() {
        /*
         * Сценарий: Получение списка пользователей простым пользователем (не администратором).
         * 1. Регистрация alice
         * 2. Авторизация alice
         * 3. Запрос GET /users
         * Ожидаемый результат: 403 Forbidden.
         */

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

        // 3. Запрос списка пользователей
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(url)
                .then()
                .statusCode(403)
                .contentType(ContentType.JSON)
                .body("code", equalTo("FORBIDDEN"));
    }


}



