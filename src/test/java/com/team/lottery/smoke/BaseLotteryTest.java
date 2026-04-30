package com.team.lottery.smoke;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Base test class providing common specifications and authentication logic.
 * Обновлено: подтягивать конфигурацию окружения из переменных и ждать готовности сервера.
 */
public abstract class BaseLotteryTest {

    protected static String adminToken;
    protected static String userToken;

    // Endpoints constants to avoid string duplication
    public static final String ENDPOINT_LOGIN = "/auth/login";
    public static final String ENDPOINT_REGISTER = "/auth/register";
    public static final String ENDPOINT_DRAWS = "/draws";
    public static final String ENDPOINT_TICKETS = "/tickets";
    public static final String ENDPOINT_HEALTH = "/health";

    @BeforeAll
    public static void setup() {
        // Базовые настройки по умолчанию (локальная среда)
        String baseUri = System.getenv().getOrDefault("API_BASE_URL", "http://localhost");
        String basePath = System.getenv().getOrDefault("API_BASE_PATH", "/api");
        int port = Integer.parseInt(System.getenv().getOrDefault("API_PORT", "8080"));

        // Разбор URL, чтобы корректно выставить baseURI и port в зависимости от окружения
        try {
            URL url = new URL(baseUri);
            RestAssured.baseURI = url.getProtocol() + "://" + url.getHost();
            RestAssured.port = (url.getPort() == -1) ? port : url.getPort();
            // если в APIBasePath указан путь, можно задать basePath отдельно
            RestAssured.basePath = basePath;
        } catch (MalformedURLException e) {
            // Фоллбек по дефолту
            RestAssured.baseURI = baseUri;
            RestAssured.port = port;
            RestAssured.basePath = basePath;
        }

        // Включаем детальное логирование при падении теста
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        // Прежде чем идти дальше, подождем, пока health endpoint станет 200 (если сервер готовится)
        waitForServerReady();

        // Initialize tokens (best-effort; null tokens are handled by request specs)
        adminToken = authenticate("admin", "adminPass");
        userToken = authenticate("user1", "userPass");
    }

    /** Wait for the server health to be UP (with timeout). */
    private static void waitForServerReady() {
        int maxSeconds = 60;
        int waited = 0;
        boolean healthy = false;
        while (waited < maxSeconds) {
            try {
                int code = RestAssured.given().get(ENDPOINT_HEALTH).getStatusCode();
                healthy = (code == 200);
                if (healthy) break;
            } catch (Exception ignored) {
                // ignore and retry
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
            waited += 2;
        }
        if (!healthy) {
            throw new IllegalStateException("Server is not ready after " + maxSeconds + " seconds");
        }
    }

    /**
     * General request specification with optional authentication.
     * Prevents issues by ensuring base configuration is solid and auth header is only sent when present.
     */
    protected static RequestSpecification requestSpec(String token) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON);

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        return builder.build();
    }

    protected static RequestSpecification adminSpec() {
        return requestSpec(adminToken);
    }

    protected static RequestSpecification userSpec() {
        return requestSpec(userToken);
    }

    protected static RequestSpecification publicSpec() {
        return requestSpec(null);
    }

    private static String authenticate(String username, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        try {
            return io.restassured.RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(credentials)
                    .post(ENDPOINT_LOGIN)
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("token");
        } catch (Exception e) {
            System.err.println("Failed to authenticate user: " + username + ". Check if server is running.");
            return null;
        }
    }
}