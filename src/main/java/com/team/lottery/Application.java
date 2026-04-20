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
import com.team.lottery.draws.repository.DrawResultRepository;
import com.team.lottery.draws.repository.InMemoryDrawResultRepository;

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

        DrawRepository drawRepository = new InMemoryDrawRepository();
        DrawResultRepository drawResultRepository = new InMemoryDrawResultRepository();
        DrawService drawService = new DrawService(drawRepository, drawResultRepository);
        DrawController drawController = new DrawController(drawService);
        AdminDrawController adminDrawController = new AdminDrawController(drawService);

        Javalin app = JavalinConfig.create(cfg.port(), routes -> {
            drawController.registerRoutes(routes);
            adminDrawController.registerRoutes(routes);
        });

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
