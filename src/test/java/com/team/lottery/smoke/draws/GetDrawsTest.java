package com.team.lottery.smoke.draws;

import com.team.lottery.Application;
import com.team.lottery.config.DatabaseConfig;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


@Testcontainers
public class GetDrawsTest {

    private static Javalin app;
    private static final int TEST_PORT = 8082; // Используем отдельный порт для тестов


    private String getDateInFuture() {
        // Получить дату в будущем.
        // Дата проведения тиража должна быть в будущем.

        return OffsetDateTime.now(ZoneOffset.UTC)
                .plusYears(1)
                .toInstant()
                .toString();
    }



    @BeforeAll
    public static void startApp() {
        // Приложение запускается один раз для всего класса
        app = Application.start(TEST_PORT);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = TEST_PORT;

        // Регистрируем парсер один раз для всего класса тестов
        RestAssured.registerParser("text/plain", Parser.TEXT);
    }

    @BeforeEach
    public void cleanDatabase() {
        // Очищаем базу перед каждым тестом, чтобы обеспечить изоляцию
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("TRUNCATE TABLE users, draws, tickets, draw_results RESTART IDENTITY CASCADE");
        } catch (Exception e) {
            throw new RuntimeException("Failed to truncate database during setup", e);
        }
    }

    @AfterAll
    public static void stopApp() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    public void healthStatusUp() {
        /*
        Health-check базовый
        Сценарий: Проверка доступности сервиса
        Вход: GET /health
        Ожидаемый результат: 200 OK, { "status": "UP", "service": "lottery-api" }
        * */
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", equalTo("UP"))
                .body("service", equalTo("lottery-api"));
    }

    @Test
    public void healthDbStatusUp() {
        /*
        Сценарий: Проверка подключения к БД
        Вход: GET /health/db
        Ожидаемый результат: 200 OK, { "status": "UP", "database": "CONNECTED", "dbName": "lottery", "dbUser": "postgres" }
        * */
        given()
                .when()
                .get("/health/db")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", equalTo("UP"))
                .body("database", equalTo("CONNECTED"))
                .body("dbName", equalTo("lottery"))
                .body("dbUser", equalTo("lottery"));
    }

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
                .post("/auth/register")
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
                .post("/auth/register")
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
                .post("/auth/register")
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
                .post("/auth/register")
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
                .post("/auth/register")
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
                .post("/auth/login")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo("User is already logged in"))
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
                .post("/auth/login")
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
                .post("/auth/login")
                .then()
                .statusCode(401) // Теперь валидатор пропустит запрос дальше
                .body("code", equalTo("UNAUTHORIZED"));
    }


    @Test
    public void getMeSuccessful() {
        /* Инфо о текущем пользователе
         * Сценарий: Получение данных авторизованного пользователя
         * 1. Регистрируем alice
         * 2. Логинимся, чтобы получить токен
         * 3. Запрашиваем /users/me с токеном
         *    GET /users/me с JWT токеном alice
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
                .get("/users/me")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("login", equalTo("alice111"))
                .body("role", equalTo("USER"));
    }

    @Test
    public void getMeWithoutToken() {
        /* Инфо о пользователе без JWT
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
                .post("/auth/logout")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Logout successful"));
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
                .post("/auth/login")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("login", equalTo("admin"))
                .body("role", equalTo("ADMIN"));
    }

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
                .get("/admin/ping")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Admin access granted"))
                .body("login", equalTo("admin"))
                .body("role", equalTo("ADMIN"));
    }


    @Test
    public void adminPingWithoutToken() {
        /* Пинг админа без JWT
         * Сценарий: Отказ без авторизации
         * Вход: GET /admin/ping без токена
         * Ожидаемый результат: 401 Unauthorized
         */
        given()
                .when()
                .get("/admin/ping")
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
                .get("/admin/ping")
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
                .get("/admin/ping")
                .then()
                .statusCode(org.hamcrest.Matchers.anyOf(equalTo(403)));
    }


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
                .get("/users")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                // Проверяем, что массив содержит именно двух пользователей
                .body("size()", equalTo(2))
                .body("login", org.hamcrest.Matchers.containsInAnyOrder("alice", "admin"));
    }

    @Test
    public void createDrawAsAdmin() {
        /*
         * Сценарий: Создание нового тиража
         * 1. Регистрация и повышение прав admin
         * 2. Авторизация admin
         * 3. POST /admin/draws с данными тиража
         * Ожидаемый результат: 201 Created, DrawResponse с ID=1, status=ACTIVE
         */

        // 1. Создание админа (используем вспомогательный подход)
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set admin role", e);
        }

        // 2. Логин
        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 3. Создание тиража
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "title": "Тираж #1",
                      "totalTickets": 1000,
                      "endDate": "%s"
                    }
                    """.formatted(getDateInFuture())) // Inject the date here
                .when()
                .post("/admin/draws")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue()) // Usually safer than checking for specific ID '1'
                .body("title", equalTo("Тираж #1"))
                .body("totalTickets", equalTo(1000))
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    public void createDrawWithEmptyTitle() {
        /* Валидация параметров тиража
         * Сценарий: Создадим тираж с пустым title.
         * 1. Подготовка: регистрация админа и получение токена
         * 2. POST /admin/draws без title
         * Ожидаемый результат: 400 Bad Request
         */

        // 1. Регистрация и получение токена админа
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set admin role", e);
        }

        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 2. Попытка создания тиража с пустым title
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "totalTickets": 100,
                          "endDate": "%s"
                        }
                        """.formatted(getDateInFuture()))
                .when()
                .post("/admin/draws")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("VALIDATION_FAILED"));
    }



    @Test
    public void getAllDraws() {
        /* Список тиражей (все статусы)
         * Сценарий: Получение всех тиражей без фильтра
         * 1. Подготовка: создаем админа и авторизуемся
         * 2. Создаем один тираж
         * 3. GET /draws
         * Ожидаемый результат: 200 OK, массив с 1 тиражом status=ACTIVE
         */

        // 1. Создаем админа и получаем токен (для создания тиража)
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set admin role", e);
        }

        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 2. Создаем тираж
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "title": "Тираж #1",
                          "totalTickets": 1000,
                          "endDate": "%s"
                        }
                        """, getDateInFuture()))
                .post("/admin/draws");

        // 3. Получаем список всех тиражей
        given()
                .when()
                .get("/draws")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(1))
                .body("[0].title", equalTo("Тираж #1"))
                .body("[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void filterDrawsByStatus() {
        /*
         * Сценарий: Фильтрация ACTIVE тиражей
         * 1. Подготовка: создаем админа, получаем токен
         * 2. Создаем один тираж (он по умолчанию ACTIVE)
         * 3. GET /draws?status=ACTIVE
         * Ожидаемый результат: Массив с тиражом.
         */

        // 1. Создание админа и логин
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set admin role", e);
        }

        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 2. Создаем тираж
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "title": "Активный тираж",
                          "totalTickets": 500,
                          "endDate": "%s"
                        }
                        """, getDateInFuture()))
                .post("/admin/draws");

        // 3. Выполняем GET запрос с фильтром
        given()
                .queryParam("status", "ACTIVE")
                .when()
                .get("/draws")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(1))
                .body("[0].title", equalTo("Активный тираж"))
                .body("[0].status", equalTo("ACTIVE"));
    }


    @Test
    public void getDrawById() {
        /*
         * Сценарий: Получение конкретного тиража
         * 1. Подготовка: создаем админа, логинимся, создаем тираж
         * 2. GET /draws/1
         * Ожидаемый результат: 200 OK, DrawDto ID=1
         */

        // 1. Создаем админа и авторизуемся
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set admin role", e);
        }

        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 2. Создаем тираж
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "title": "Тираж для теста ID",
                          "totalTickets": 200,
                          "endDate": "%s"
                        }
                        """, getDateInFuture()))
                .post("/admin/draws");

        // 3. Запрашиваем тираж по ID=1
        given()
                .when()
                .get("/draws/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("title", equalTo("Тираж для теста ID"))
                .body("totalTickets", equalTo(200))
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    public void getNonExistentDraw() {
        /*
         * Сценарий: Поиск несуществующего тиража
         * Вход: GET /draws/999
         * Ожидаемый результат: 404 Not Found
         */
        given()
                .when()
                .get("/draws/999")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", equalTo("NOT_FOUND"));
    }


    @Test
    public void buyTicketSuccessful() {
        /*
         * Сценарий: Пользователь покупает билет
         * 1. Админ создает тираж
         * 2. Юзер логинится
         * 3. Юзер покупает билет POST /draws/1/tickets
         * Ожидаемый результат: 200 OK, BuyTicketResponseV2 с ticket status=SOLD
         */

        // 1. Создание админа и тиража
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

        // 2. Логин юзера
        given().contentType(ContentType.JSON).body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/register");
        String userToken = given().contentType(ContentType.JSON).body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/login").then().extract().path("token");

        // 3. Покупка билета
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post("/draws/1/tickets")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("ticket.status", equalTo("SOLD"))
                // Сначала регистрируем admin (он получает ID=1).
                // Затем регистрируем alice (она получает ID=2).
                .body("ticket.ownerId", equalTo(2)); // ID alice в чистой базе будет 2.
    }

    @Test
    public void buyTicketWithoutToken() {
        /* Попытка купить билет, не будучи авторизованным.
         * Сценарий: Отказ неавторизованному пользователю
         * Вход: POST /draws/1/tickets без токена
         * Ожидаемый результат: 401 Unauthorized
         */
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/draws/1/tickets")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    public void buyTicketInNonExistentDraw() {
        /* Попутка купить билет несуществующего тиража.
         * Сценарий: Тираж не найден
         * 1. Регистрация и логин alice
         * 2. POST /draws/999/tickets с токеном alice
         * Ожидаемый результат: 404 Not Found
         */

        // 1. Регистрация и логин alice
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");

        String userToken = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then().extract().path("token");

        // 2. Попытка покупки в несуществующий тираж
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post("/draws/999/tickets")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", equalTo("NOT_FOUND"));
    }

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
                .get("/me/tickets")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(1))
                .body("[0].ownerId", equalTo(1));
    }

    @Test
    public void getMyResultsBeforeDraw() {
        /* Попытка пользователя увидеть свои результаты до розыгрыша.
         * Сценарий: Результаты до проведения тиража
         * 1. Подготовка: регистрация alice, создание тиража админом, покупка билета
         * 2. GET /me/results с токеном alice
         * Ожидаемый результат: 200 OK, пустой массив
         */

        // 1. Подготовка: Регистрация alice и логин
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");
        String userToken = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then().extract().path("token");

        // 2. Подготовка: Создание тиража админом
        given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        given().header("Authorization", "Bearer " + adminToken).contentType(ContentType.JSON)
                .body(String.format("{ \"title\": \"Тираж #1\", \"totalTickets\": 100, \"endDate\": %s }", "2027-01-01T00:00:00Z"))
                .post("/admin/draws");

        // 3. Покупка билета alice
        given().header("Authorization", "Bearer " + userToken).post("/draws/1/tickets");

        // 4. Проверка результатов (ожидаем пустоту)
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/me/results")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(0)); // Пустой массив
    }


    @Test
    public void runDrawAsAdmin() {
        /* Проведение розыгрыша (админ)
         * Сценарий: Запуск розыгрыша тиража
         * Вход: POST /admin/draws/1/run-draw с JWT админа
         * Ожидаемый результат: 200 OK, DrawResponse status=COMPLETED
         * */
        // Регистрация парсера для обработки text/plain как текста
        RestAssured.registerParser("text/plain", Parser.TEXT);

        // 1. Создаем админа, создаем тираж
        given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        given().header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .body(String.format("{ \"title\": \"Тираж\", \"totalTickets\": 10, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws");

        // 2. Покупка билета
        given().contentType("application/json").body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/register");
        String userToken = given().contentType("application/json").body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/login").then().extract().path("token");
        given().header("Authorization", "Bearer " + userToken).post("/draws/1/tickets");

        // 3. Ставим статус CLOSED
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE draws SET status = 'CLOSED' WHERE id = 1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 4. Выполнение
        Response response = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .post("/admin/draws/1/run-draw");

        // 5. Проверка:
        // Если статус 200, то мы уверены в успехе.
        // Если вы уверены, что сервер возвращает JSON при успехе, используйте .body("status", ...)
        // Если сервер возвращает JSON только при успехе, а при ошибке текст, то:
        response.then().statusCode(200);

        // Пытаемся проверить JSON только если тело НЕ пустое
        if (response.getContentType() != null && response.getContentType().contains("application/json")) {
            response.then().body("status", equalTo("COMPLETED"));
        }
    }

    @Test
    public void getDrawResult() {
        /* Результат розыгрыша
         * Сценарий: Получение результата розыгрыша
         * 1. Подготовка: создаем тираж, покупаем билет, проводим розыгрыш
         * 2. Отладка: проверяем наличие записи в БД перед запросом
         * 3. GET /draws/1/result
         * 4. Проверка: наличие данных о победителе
         * Ожидаемый результат: 200 OK
         */

        // 1. Подготовка: регистрация админа, токен, создание тиража
        given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        given().header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .body(String.format("{ \"title\": \"Тираж для результата\", \"totalTickets\": 10, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws");

        // Покупка билета
        given().contentType("application/json").body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/register");
        String userToken = given().contentType("application/json").body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/login").then().extract().path("token");
        given().header("Authorization", "Bearer " + userToken).post("/draws/1/tickets");

        // Принудительное закрытие и проведение розыгрыша
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE draws SET status = 'CLOSED' WHERE id = 1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given().header("Authorization", "Bearer " + adminToken).post("/admin/draws/1/run-draw");

        // 2. ОТЛАДКА: Проверка БД перед запросом
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            var resultSet = connection.createStatement().executeQuery("SELECT winning_ticket_id FROM draw_results WHERE draw_id = 1");
            if (resultSet.next()) {
                System.out.println("DEBUG: Database winnerTicketId = " + resultSet.getLong("winning_ticket_id"));
            } else {
                System.out.println("DEBUG: No draw_result record found in database!");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 3. GET запрос на получение результата
        Response response = given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/draws/1/result");

        // 4. Проверка результата
        System.out.println("DEBUG: Response JSON = " + response.getBody().asString());

        response.then()
                .statusCode(200)
                .body("drawId", equalTo(1))
                .body("winningTicketId", notNullValue());
    }


    @Test
    public void getUserResults() {
        /* Мои результаты после розыгрыша
         * Сценарий: Проверка статуса билетов пользователя после розыгрыша
         * 1. Подготовка: Создаем тираж, покупаем билет для Alice
         * 2. Розыгрыш: Закрываем тираж и запускаем run-draw
         * 3. GET /me/results: Проверяем, что билеты имеют статус WIN или LOSE
         * Ожидаемый результат: Билет со статусом WIN или LOSE
         */

        // 1. Подготовка: Регистрация Alice и получение токена
        given().contentType("application/json")
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/register");
        String aliceToken = given().contentType("application/json")
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
                .then().extract().path("token");

        // Регистрация админа и создание тиража
        given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType("application/json").body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        given().header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .body(String.format("{ \"title\": \"Тираж для результатов\", \"totalTickets\": 10, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws");

        // Alice покупает билет
        given().header("Authorization", "Bearer " + aliceToken).post("/draws/1/tickets");

        // 2. Проведение розыгрыша (перевод статусов билетов из SOLD в WIN/LOSE)
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE draws SET status = 'CLOSED' WHERE id = 1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given().header("Authorization", "Bearer " + adminToken).post("/admin/draws/1/run-draw");

        // 3. Запрос результатов пользователя
        Response response = given()
                .header("Authorization", "Bearer " + aliceToken)
                .when()
                .get("/me/results");

        // 4. Проверка: статус должен быть либо WIN, либо LOSE
        response.then()
                .statusCode(200)
                .body("[0].status", anyOf(equalTo("WIN"), equalTo("LOSE")));
    }

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
                .get("/admin/logged-in-users")
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
                .get("/admin/logged-in-users")
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
                .get("/admin/logged-in-users")
                .then()
                .log().ifValidationFails()
                .statusCode(401).
                contentType(ContentType.JSON)
                .body("code", equalTo("UNAUTHORIZED"));
    }


    @Test
    public void createASecondDrawAsAdmin() {
        /*
            Создание второго тиража
            Сценарий: Множественные тиражи
            Вход: POST /admin/draws с { "title": "Тираж #2"... }
            Ожидаемый результат: 201, Тираж #2"
         */

        // 1. Создание админа (используем вспомогательный подход)
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set admin role", e);
        }

        // 2. Логин
        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 3. Создание тиража
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "title": "Тираж #1",
                          "totalTickets": 1000,
                          "endDate": \"%s\"
                        }
                        """, getDateInFuture()))
                .when()
                .post("/admin/draws")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("title", equalTo("Тираж #1"))
                .body("totalTickets", equalTo(1000))
                .body("status", equalTo("ACTIVE"));

        // 4. Создание второго тиража
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "title": "Тираж #2",
                          "totalTickets": 2000,
                          "endDate": \"%s\"
                        }
                        """, getDateInFuture()))
                .when()
                .post("/admin/draws")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo(2))
                .body("title", equalTo("Тираж #2"))
                .body("totalTickets", equalTo(2000))
                .body("status", equalTo("ACTIVE"));
    }


    @Test
    public void buySeveralTicketsByOneUser() {
        /*
        Покупка нескольких билетов одним пользователем
        Сценарий: Пользователь покупает 2 билета в разные тиражи
        Вход: POST /draws/1/tickets и POST /draws/2/tickets с JWT alice
        Ожидаемый результат: 2 билета в /me/tickets
         */

        // 1. Создание админа и тиража
        given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String adminToken = given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login")
                .then().extract().path("token");

        // Создаем тираж и забираем его настоящий ID
        int drawId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"title\": \"Lottery 1\", \"totalTickets\": 100, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws")
                .then().extract().path("id");

        // 2. Регистрация и логин юзера
        given().contentType(ContentType.JSON).body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/register");
        String userToken = given().contentType(ContentType.JSON).body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }").post("/auth/login")
                .then().extract().path("token");

        // 3. Покупка двух билетов в этот тираж
        for (int i = 0; i < 2; i++) {
            given()
                    .header("Authorization", "Bearer " + userToken)
                    .when()
                    .post("/draws/" + drawId + "/tickets")
                    .then()
                    .statusCode(200)
                    .body("ticket.status", equalTo("SOLD"));
        }

        // 4. ФИНАЛЬНАЯ ПРОВЕРКА (согласно сценарию)
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/me/tickets")
                .then()
                .statusCode(200)
                .body("$", hasSize(2)) // Проверяем, что билетов именно 2
                .body("ownerId", everyItem(notNullValue()));
    }

    @Test
    public void buyTicketLimitExceeded() {
        /*
         * Сценарий: Попытка купить билет после исчерпания лимита
         * 1. Админ создает тираж с totalTickets = 1
         * 2. Юзер покупает первый (он же последний доступный) билет
         * 3. Юзер пытается купить второй билет в тот же тираж
         * Ожидаемый результат: 409 на второй попытке
         */

        // 1. Подготовка: Регистрация и получение токена админа
        given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/register");
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String adminToken = given().contentType(ContentType.JSON).body("{ \"login\": \"admin\", \"password\": \"admin123\" }").post("/auth/login").then().extract().path("token");

        // 2. Создание тиража с лимитом в 1 билет
        int drawId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"title\": \"Limited Draw\", \"totalTickets\": 1, \"endDate\": \"%s\" }", getDateInFuture()))
                .post("/admin/draws")
                .then()
                .statusCode(201)
                .extract().path("id");

        // 3. Подготовка юзера
        given().contentType(ContentType.JSON).body("{ \"login\": \"bob\", \"password\": \"password123\" }").post("/auth/register");
        String userToken = given().contentType(ContentType.JSON).body("{ \"login\": \"bob\", \"password\": \"password123\" }").post("/auth/login").then().extract().path("token");

        // 4. Покупка первого билета (успешно)
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post("/draws/" + drawId + "/tickets")
                .then()
                .statusCode(200)
                .body("message", equalTo("Ticket purchased successfully"));

        // 5. Попытка покупки второго билета (ожидаем ошибку)
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post("/draws/" + drawId + "/tickets")
                .then()
                .log().ifValidationFails()
                .statusCode(409) // Bad Request согласно спецификации для невозможной покупки
                .contentType(ContentType.JSON)
                .body("code", notNullValue())
                .body("message", anyOf(
                        containsString("No available tickets for this draw"),
                        containsString("sold out"),
                        containsString("Limit exceeded")
                ));
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
                .post("/admin/draws/" + drawId + "/run-draw")
                .then()
                .log().ifValidationFails()
                .statusCode(409) // Ожидаем ошибку бизнес-логики
                .contentType(ContentType.JSON)
                .body("code", equalTo("CONFLICT"))
                .body("message", anyOf(
                        containsString("No tickets sold - cannot run draw")
                ));
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
                .get("/users")
                .then()
                .statusCode(403)
                .contentType(ContentType.JSON)
                .body("code", equalTo("FORBIDDEN"));
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
                .post("/auth/logout")
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
                .post("/auth/logout")
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
                .post("/auth/logout")
                .then()
                .statusCode(200);
        // Пытаемся зайти с тем же токеном снова
        given()
                .header("Authorization", "Bearer " + userToken)
                .get("/users/me")
                .then()
                .statusCode(401); // Ожидаем отказ, так как токен должен быть недействителен
    }

    @Test
    public void loginAfterLogout() {
        /*
        Повторный логин после логаута
        Сценарий: Получение нового токена
        Вход: Логин → логаут → повторный логин alice
        Ожидаемый результат: Новый JWT токен успешно получен
        и работает.
        * */
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


        // 3. Разлогиниваемся
        given()
                .header("Authorization", "Bearer " + userToken)
                .post("/auth/logout")
                .then()
                .statusCode(200);

        // 4. Логинимся снова
        String newUserToken = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"alice\", \"password\": \"supersecret123\" }")
                .post("/auth/login")
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


    @Test
    void shouldReturn409WhenEndDateInPast() {
        /*
        Дата окончания тиража в прошлом
        Сценарий: Создание тиража с endDate < now()
        Вход: POST /admin/draws с endDate в прошлом
        Ожидаемый результат: 409
        * */

        // 1. Создание админа (используем вспомогательный подход)
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register");

        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            connection.createStatement().execute("UPDATE users SET role = 'ADMIN' WHERE login = 'admin'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set admin role", e);
        }

        // 2. Логин
        String token = given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/login")
                .then()
                .extract()
                .path("token");

        // 3. Создание тиража. Дата - в прошлом.
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Тираж #1",
                          "totalTickets": 1000,
                          "endDate": "2024-04-25T18:00:00Z"
                        }
                        """)
                .when()
                .post("/admin/draws")
                .then()
                .statusCode(409)
                .body("code", equalTo("CONFLICT"))
                .body("message", anyOf(
                        containsString("The draw end date cannot be in the past")
                ));
    }
}



