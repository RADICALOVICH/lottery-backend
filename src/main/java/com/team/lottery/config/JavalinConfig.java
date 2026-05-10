package com.team.lottery.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.lottery.common.errors.ErrorHandler;
import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import io.javalin.json.JavalinJackson;
import io.javalin.http.staticfiles.Location;

import java.util.function.Consumer;

public final class JavalinConfig {

    private JavalinConfig() {
    }

    public static Javalin create(boolean isProd,
                                 int port,
                                 Consumer<RoutesConfig> routesRegister) {
        return Javalin.create(config -> {
            config.jetty.port = port;
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });

            // Swagger UI (:8090) и api-demo делают кросс-origin запросы к API (:8080).
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    if (!isProd) {
                        it.anyHost();
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