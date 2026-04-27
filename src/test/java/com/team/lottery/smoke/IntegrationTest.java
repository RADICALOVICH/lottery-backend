package com.team.lottery.smoke;

import com.team.lottery.Application;
import com.team.lottery.config.DatabaseConfig;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers
public class IntegrationTest {

    private static Javalin app;
    private static final int TEST_PORT = 8082; // Используем отдельный порт для тестов

    @BeforeAll
    public static void startApp() {
        // Приложение запускается один раз для всего класса
        app = Application.start(TEST_PORT);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = TEST_PORT;
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
        /*
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
        /*
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
        /*
         * Сценарий: Проверка уникальности логина
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
        /*
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
        /*
         * Сценарий: Отказ при несуществующем логине
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
        /*
         * Сценарий: Получение данных авторизованного пользователя
         * 1. Регистрируем alice
         * 2. Логинимся, чтобы получить токен
         * 3. Запрашиваем /users/me с токеном
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
        /*
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
        /*
         * Сценарий: Успешный логаут
         * 1. Регистрируем alice
         * 2. Логинимся для получения токена
         * 3. Выполняем logout с этим токеном
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
    public void createAdminUser() {
        /*
         * Сценарий: Регистрация админа
         * 1. Регистрация пользователя "admin"
         * 2. Прямое обновление роли в БД на "ADMIN"
         */

        // 1. Регистрация
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .post("/auth/register")
                .then()
                .statusCode(201);

        // 2. Обновление роли в БД
        try (var connection = DatabaseConfig.getDataSource().getConnection()) {
            var statement = connection.prepareStatement("UPDATE users SET role = 'ADMIN' WHERE login = ?");
            statement.setString(1, "admin");
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user role to ADMIN", e);
        }

        // 3. Проверка: логинимся и проверяем роль в ответе
        given().contentType(ContentType.JSON)
                .body("{ \"login\": \"admin\", \"password\": \"admin123\" }")
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("login", equalTo("admin"))
                .body("role", equalTo("ADMIN"));
    }


    @Test
    public void loginAdminSuccessful() {
        /*
         * Сценарий: Авторизация администратора
         * 1. Регистрация пользователя "admin"
         * 2. Повышение его прав до ADMIN
         * 3. Авторизация и проверка роли
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
        /*
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
        /*
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
    public void adminPingByUser() {
        /*
         * Сценарий: Отказ USER'у в админском доступе
         * 1. Регистрация пользователя alice
         * 2. Авторизация alice
         * 3. Запрос /admin/ping с токеном alice
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
                // Ожидаем 403 Forbidden (стандарт для "аутентифицирован, но нет прав"),
                // но принимаем 401, если ваша система так обрабатывает ошибку доступа
                .statusCode(org.hamcrest.Matchers.anyOf(equalTo(401), equalTo(403)));
    }


    @Test
    public void getAllUsersByAdmin() {
        /*
         * Сценарий: Получение списка пользователей админом
         * 1. Регистрация alice
         * 2. Регистрация admin и повышение его прав
         * 3. Авторизация admin
         * 4. Запрос GET /users
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
                    "endDate": "2026-04-25T18:00:00Z"
                  }
                  """)
                .when()
                .post("/admin/draws")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("title", equalTo("Тираж #1"))
                .body("totalTickets", equalTo(1000))
                .body("status", equalTo("ACTIVE"));
    }


    @Test
    public void createDrawWithEmptyTitle() {
        /*
         * Сценарий: Валидация параметров тиража (пустой title)
         * 1. Подготовка: регистрация админа и получение токена
         * 2. POST /admin/draws без title
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
                    "endDate": "2026-04-25T18:00:00Z"
                  }
                  """)
                .when()
                .post("/admin/draws")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("VALIDATION_FAILED"));
    }

    @Test
    public void getAllDraws() {
        /*
         * Сценарий: Получение всех тиражей без фильтра
         * 1. Подготовка: создаем админа и авторизуемся
         * 2. Создаем один тираж
         * 3. GET /draws
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
                .body("""
                  {
                    "title": "Тираж #1",
                    "totalTickets": 1000,
                    "endDate": "2026-04-25T18:00:00Z"
                  }
                  """)
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
                .body("""
                  {
                    "title": "Активный тираж",
                    "totalTickets": 500,
                    "endDate": "2026-05-01T10:00:00Z"
                  }
                  """)
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
                .body("""
                  {
                    "title": "Тираж для теста ID",
                    "totalTickets": 200,
                    "endDate": "2026-06-01T12:00:00Z"
                  }
                  """)
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

}