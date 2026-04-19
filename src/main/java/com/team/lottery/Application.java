package com.team.lottery;

import com.team.lottery.config.AppConfig;
import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.config.JavalinConfig;
import com.team.lottery.draws.controller.AdminDrawController;
import com.team.lottery.draws.controller.DrawController;
import com.team.lottery.draws.repository.DrawRepository;
import com.team.lottery.draws.repository.InMemoryDrawRepository;
import com.team.lottery.draws.service.DrawService;
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

        Javalin app = JavalinConfig.create(cfg.port());

        DrawRepository drawRepository = new InMemoryDrawRepository();
        DrawService drawService = new DrawService(drawRepository);
        DrawController drawController = new DrawController(drawService);
        AdminDrawController adminDrawController = new AdminDrawController(drawService);

        drawController.registerRoutes(app);
        adminDrawController.registerRoutes(app);

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
