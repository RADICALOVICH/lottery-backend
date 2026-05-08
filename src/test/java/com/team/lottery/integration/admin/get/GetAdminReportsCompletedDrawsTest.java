package com.team.lottery.integration.admin.get;

import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.integration.BaseTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;

@Testcontainers
public class GetAdminReportsCompletedDrawsTest extends BaseTest {

    private static final String URL = "/admin/reports/draws/completed";

    /**
     * Поднимает один завершённый тираж: admin создаёт тираж, alice покупает
     * билет, тираж закрывается через БД, admin запускает розыгрыш.
     * Возвращает токен admin'а для последующих запросов отчёта.
     */
    private String setupCompletedDrawAndGetAdminToken() {
        // 1. Регистрируем админа и повышаем роль через БД
        given().contentType(ContentType.JSON)
                .body(Map.of("login", "admin", "password", "admin-pass"))
                .post("/auth/register").then().statusCode(201);

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String adminToken = given().contentType(ContentType.JSON)
                .body(Map.of("login", "admin", "password", "admin-pass"))
                .post("/auth/login")
                .then().statusCode(200)
                .extract().path("token");

        // 2. Админ создаёт тираж
        int drawId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"title\": \"Report Draw\", \"totalTickets\": 1, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws")
                .then().statusCode(201)
                .extract().path("id");

        // 3. Регистрируем alice и покупаем 1 билет
        given().contentType(ContentType.JSON)
                .body(Map.of("login", "alice", "password", "password123"))
                .post("/auth/register").then().statusCode(201);
        String aliceToken = given().contentType(ContentType.JSON)
                .body(Map.of("login", "alice", "password", "password123"))
                .post("/auth/login")
                .then().statusCode(200)
                .extract().path("token");
        given().header("Authorization", "Bearer " + aliceToken)
                .post("/draws/" + drawId + "/tickets")
                .then().statusCode(200);

        // 4. Закрываем тираж через БД (правило: розыгрыш только из CLOSED)
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            var statement = connection.prepareStatement("UPDATE draws SET status = 'CLOSED' WHERE id = ?");
            statement.setInt(1, drawId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 5. Админ запускает розыгрыш → статус COMPLETED + draw_results
        given().header("Authorization", "Bearer " + adminToken)
                .post("/admin/draws/" + drawId + "/run-draw")
                .then().statusCode(200)
                .body("status", equalTo("COMPLETED"));

        return adminToken;
    }

    @Test
    public void getReportAsAdminJson() {
        /*
         * Сценарий: GET /admin/reports/draws/completed с дефолтным форматом (json).
         * Ожидаемый результат: 200 JSON-массив с одним завершённым тиражом,
         *                      поля заполнены (winnerLogin = "alice").
         */
        String adminToken = setupCompletedDrawAndGetAdminToken();

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get(URL)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Report Draw"))
                .body("[0].totalTickets", equalTo(1))
                .body("[0].soldTickets", equalTo(1))
                .body("[0].winnerLogin", equalTo("alice"))
                .body("[0].createdByAdminLogin", equalTo("admin"));
    }

    @Test
    public void getReportAsAdminCsv() {
        /*
         * Сценарий: GET /admin/reports/draws/completed?format=csv.
         * Ожидаемый результат: 200, content-type text/csv, Content-Disposition с
         *                      именем файла completed-draws-YYYYMMDD-HHmm.csv,
         *                      тело содержит шапку и одну строку данных.
         */
        String adminToken = setupCompletedDrawAndGetAdminToken();

        Response response = given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("format", "csv")
                .when()
                .get(URL)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(startsWith("text/csv"))
                .header("Content-Disposition",
                        matchesPattern("attachment; filename=\"completed-draws-\\d{8}-\\d{4}\\.csv\""))
                .extract().response();

        String body = response.asString();
        // Шапка + хотя бы одна data-строка
        assert body.startsWith("drawId,title,createdAt,endDate,totalTickets,soldTickets,winnerTicketNumber")
                : "CSV header mismatch: " + body;
        assert body.contains("Report Draw") : "CSV missing draw title: " + body;
        assert body.contains("alice") : "CSV missing winner login: " + body;
    }

    @Test
    public void getReportAsUser() {
        /*
         * Сценарий: запрос отчёта обычным пользователем.
         * Ожидаемый результат: 403 FORBIDDEN.
         */
        given().contentType(ContentType.JSON)
                .body(Map.of("login", "bob", "password", "password123"))
                .post("/auth/register").then().statusCode(201);
        String userToken = given().contentType(ContentType.JSON)
                .body(Map.of("login", "bob", "password", "password123"))
                .post("/auth/login")
                .then().statusCode(200)
                .extract().path("token");

        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(URL)
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .contentType(ContentType.JSON)
                .body("code", equalTo("FORBIDDEN"));
    }

    @Test
    public void getReportWithInvalidFormat() {
        /*
         * Сценарий: GET с ?format=xml.
         * Ожидаемый результат: 400 VALIDATION_ERROR с сообщением про допустимые
         *                      значения 'csv' и 'json'.
         */
        // Минимальный admin (без тиража — до проверки формата дело не дойдёт)
        given().contentType(ContentType.JSON)
                .body(Map.of("login", "admin", "password", "admin-pass"))
                .post("/auth/register").then().statusCode(201);

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String adminToken = given().contentType(ContentType.JSON)
                .body(Map.of("login", "admin", "password", "admin-pass"))
                .post("/auth/login")
                .then().statusCode(200)
                .extract().path("token");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("format", "xml")
                .when()
                .get(URL)
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("message", containsString("csv"))
                .body("message", containsString("json"));
    }
}
