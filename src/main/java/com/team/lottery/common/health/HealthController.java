package com.team.lottery.common.health;

import io.javalin.config.RoutesConfig;

import javax.sql.DataSource;
import java.util.Map;

public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void registerRoutes(RoutesConfig routes) {
        routes.get("/health", ctx -> ctx.status(200).json(Map.of(
                "status", "UP",
                "service", "lottery-api"
        )));

        routes.get("/health/db", ctx -> {
            try (var connection = dataSource.getConnection()) {
                boolean valid = connection.isValid(2);

                String url = connection.getMetaData().getURL();
                String user = connection.getMetaData().getUserName();
                String dbName = extractDatabaseName(url);

                if (valid) {
                    ctx.status(200).json(Map.of(
                            "status", "UP",
                            "database", "CONNECTED",
                            "dbName", dbName,
                            "dbUser", user
                    ));
                } else {
                    ctx.status(503).json(Map.of(
                            "status", "DOWN",
                            "database", "DISCONNECTED",
                            "dbName", dbName,
                            "dbUser", user
                    ));
                }
            } catch (Exception e) {
                ctx.status(503).json(Map.of(
                        "status", "DOWN",
                        "database", "DISCONNECTED",
                        "error", e.getMessage()
                ));
            }
        });
    }

    private String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "unknown";
        }

        int slashIndex = jdbcUrl.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex == jdbcUrl.length() - 1) {
            return "unknown";
        }

        String dbPart = jdbcUrl.substring(slashIndex + 1);
        int queryIndex = dbPart.indexOf('?');
        if (queryIndex >= 0) {
            dbPart = dbPart.substring(0, queryIndex);
        }

        return dbPart;
    }
}