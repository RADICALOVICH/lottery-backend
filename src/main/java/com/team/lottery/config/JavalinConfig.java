package com.team.lottery.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.lottery.common.errors.ErrorHandler;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.config.RoutesConfig;
import java.util.function.Consumer;

import java.util.Map;


/**
 * Factory for configured Javalin instance.
 */
public final class JavalinConfig {

    private JavalinConfig() {
    }

    public static Javalin create(int port, Consumer<RoutesConfig> routesRegister) {
        return Javalin.create(config -> {
            config.jetty.port = port;

            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));

            config.routes.get("/health", ctx ->
                    ctx.json(Map.of("status", "ok"))
            );

            routesRegister.accept(config.routes);

            ErrorHandler.register(config);
        });
    }
}