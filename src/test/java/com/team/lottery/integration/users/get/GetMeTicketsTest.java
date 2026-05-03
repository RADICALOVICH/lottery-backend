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
public class GetMeTicketsTest extends BaseTest {

    String url = "/me/tickets";

    @Test
    public void getMyTicketsSuccessful() {
        /*
         * Сценарий: Просмотр купленных билетов
         * 1. Регистрация и логин alice (ID=1)
         * 2. Админ создает тираж
         * 3. Alice покупает билет
         * 4. GET /me/tickets
         * Ожидаемый результат: 200 OK, массив с 1 билетом ownerId=1
         */

        // 1. Регистрация и логин alice
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");

        String userToken = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then().extract().path("token");

        // 2. Создание тиража админом
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

        // 3. Покупка билета
        given().header("Authorization", "Bearer " + userToken)
                .when().post("/draws/1/tickets");

        // 4. Проверка списка моих билетов
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(1))
                .body("[0].ownerId", equalTo(1));
    }

}



