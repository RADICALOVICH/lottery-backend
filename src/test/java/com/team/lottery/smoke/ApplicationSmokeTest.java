package com.team.lottery.smoke;



import com.team.lottery.Application;
import io.javalin.Javalin;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


class ApplicationSmokeTest {
    /*
    * Smoke-тест приложения
    * */

    private static Javalin app;
    private static final int TEST_PORT = 8082;
    private final OkHttpClient client = new OkHttpClient();

    @BeforeAll
    static void setUp() {
        // Запускаем приложение на тестовом порту
        app = Application.start(TEST_PORT);
    }

    @AfterAll
    static void tearDown() {
        app.stop();
    }

    @Test
    @DisplayName("Сервер должен отвечать на корневой эндпоинт")
    void shouldReturnWelcomeMessage() throws IOException {
        Request request = new Request.Builder()
                .url("http://localhost:" + TEST_PORT + "/")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).isEqualTo("Server is running");
        }
    }

    @Test
    @DisplayName("Эндпоинт HealthCheck должен быть доступен")
    void shouldReturnHealthStatus() throws IOException {
        Request request = new Request.Builder()
                .url("http://localhost:" + TEST_PORT + "/health")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            // Проверяем, что возвращается JSON со статусом UP
            assertThat(response.body().string()).contains("UP");
        }
    }
}