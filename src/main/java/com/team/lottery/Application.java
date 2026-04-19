package com.team.lottery;

import com.team.lottery.config.AppConfig;
import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.config.JavalinConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Application entry point.
 */
public final class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private Application() {
    }

    public static void main(String[] args) {
        AppConfig cfg = AppConfig.load();
        DataSource ds = DatabaseConfig.init(cfg);
        //DataSource ds = null; //work without docker
        Javalin app = JavalinConfig.create(cfg.port());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            app.stop();

            if (ds instanceof HikariDataSource hds) {
                hds.close();
            }
        }, "app-shutdown"));

        app.start();
        log.info("Application started on port {}", cfg.port());
    }
}