package com.team.lottery.integration.admin.post;



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
public class PostAdminDrawsIdRunDrawTest extends BaseTest {

    private String getUrl(int id) {
        return String.format("/admin/draws/%d/run-draw", id);
    }



    @Test
    public void runDrawWithoutSoldTickets() {
        /*
         * Сценарий: Проведение розыгрыша победителей без продаж билетов.
         * 1. Админ создает тираж с totalTickets = 10
         * 2. Переводим тираж в статус CLOSED (так как розыгрыш возможен только для закрытых тиражей)
         * 3. Пытаемся запустить розыгрыш эндпоинтом /admin/draws/{id}/run-draw
         * Ожидаемый результат: 409, так как нельзя выбрать победителя из 0 билетов.
         */

        // 1. Подготовка: Регистрация и получение токена админа
        given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        // 2. Создание тиража (билеты НЕ покупаем)
        int drawId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"title\": \"Empty Draw\", \"totalTickets\": 10, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws")
                .then()
                .statusCode(201)
                .extract().path("id");

        // 3. Перевод тиража в статус CLOSED в БД
        // Тираж должен быть закрыт перед розыгрышем
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            var statement = connection.prepareStatement("UPDATE draws SET status = 'CLOSED' WHERE id = ?");
            statement.setInt(1, drawId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 4. Попытка провести розыгрыш без проданных билетов
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .post(getUrl(1))
                .then()
                .log().ifValidationFails()
                .statusCode(409) // Ожидаем ошибку бизнес-логики
                .contentType(ContentType.JSON)
                .body("code", equalTo("CONFLICT"))
                .body("message", anyOf(
                        containsString("No tickets sold - cannot run draw")
                ));
    }

}



