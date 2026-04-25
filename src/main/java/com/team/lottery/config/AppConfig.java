package com.team.lottery.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Конфигурация приложения.
 *
 * Источники (в порядке возрастания приоритета):
 *   1. src/main/resources/application.properties — дефолты.
 *   2. Переменные окружения (APP_PORT, DB_URL, ...) — перекрывают дефолты.
 *
 * Имена ENV-переменных совпадают с .env.example.
 */
public record AppConfig(
        int port,
        String dbUrl,
        String dbUser,
        String dbPassword,
        int dbPoolSize,
        int bcryptCost,
        int drawSchedulerIntervalSeconds,
        boolean isProd
) {

    private static final String PROPERTIES_FILE = "application.properties";

    public static AppConfig load() {
        Properties props = loadProperties();

        return new AppConfig(
                getInt("app.port", "APP_PORT", props),
                getString("db.url", "DB_URL", props),
                getString("db.user", "DB_USER", props),
                getString("db.password", "DB_PASSWORD", props),
                getInt("db.poolSize", "DB_POOL_SIZE", props),
                getInt("bcrypt.cost", "BCRYPT_COST", props),
                getInt("draw.scheduler.intervalSeconds", "DRAW_SCHEDULER_INTERVAL_SECONDS", props),
                getBoolean("app.isProd", "APP_IS_PROD", props)
        );
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        ClassLoader cl = AppConfig.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Не найден " + PROPERTIES_FILE + " в classpath. Проверь сборку."
                );
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать " + PROPERTIES_FILE, e);
        }
        return props;
    }

    private static String getString(String propKey, String envName, Properties props) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromProps = props.getProperty(propKey);
        if (fromProps == null) {
            throw new IllegalStateException(
                    "Не задано значение для " + propKey + " (ENV: " + envName + ")"
            );
        }
        return fromProps;
    }

    private static int getInt(String propKey, String envName, Properties props) {
        String raw = getString(propKey, envName, props);
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Не число в " + propKey + " (ENV: " + envName + "): '" + raw + "'"
            );
        }
    }

    private static boolean getBoolean(String propKey, String envName, Properties props) {
        String raw = getString(propKey, envName, props);
        return Boolean.parseBoolean(raw.trim());
    }
}
