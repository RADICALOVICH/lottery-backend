package com.team.lottery.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.lottery.common.errors.ErrorHandler;
import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import io.javalin.json.JavalinJackson;

import java.util.function.Consumer;

public final class JavalinConfig {

    private JavalinConfig() {
    }

    public static Javalin create(boolean isProd,
                                 int port,
                                 Consumer<RoutesConfig> routesRegister) {
        return Javalin.create(config -> {
            config.jetty.port = port;

            // Необходимо для корректной работы Swagger.
            // Настройка CORS для сред разработки и тестирования.
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    if (!isProd) {
                        it.anyHost(); // Вне продуктивной среды разрешаем всё для удобства.
                    }
                });
            });


            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));
            routesRegister.accept(config.routes);
            ErrorHandler.register(config);
        });
    }
}