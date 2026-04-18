package com.team.lottery.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.lottery.common.errors.ErrorHandler;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

/**
 * Фабрика готового Javalin:
 *   - Jackson с поддержкой java.time (Instant и т.п.), даты — в ISO-строку.
 *   - Подключён ErrorHandler: ApiException → HTTP-код + ErrorResponse.
 *
 * Роуты тут не регистрируем — это делают Application / Wiring.
 */
public final class JavalinConfig {

    private JavalinConfig() {
    }

    public static Javalin create() {
        ObjectMapper mapper = buildObjectMapper();

        Javalin app = Javalin.create(cfg -> {
            cfg.jsonMapper(new JavalinJackson(mapper, true));
        });

        ErrorHandler.register(app);
        return app;
    }

    private static ObjectMapper buildObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
