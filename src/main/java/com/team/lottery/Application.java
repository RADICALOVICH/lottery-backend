package com.team.lottery;

import com.team.lottery.config.AppConfig;
import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.config.JavalinConfig;
import com.team.lottery.draws.controller.AdminDrawController;
import com.team.lottery.draws.controller.DrawController;
import com.team.lottery.draws.repository.*;
import com.team.lottery.draws.repository.DrawResultRepository;
import com.team.lottery.draws.scheduler.DrawScheduler;
import com.team.lottery.draws.service.DrawService;
import com.team.lottery.ticket.controller.TicketController;
import com.team.lottery.ticket.repository.TicketJdbcRepository;
import com.team.lottery.ticket.repository.TicketRepository;
import com.team.lottery.ticket.service.TicketService;
import com.team.lottery.users.controller.AuthController;
import com.team.lottery.users.controller.UserController;
import com.team.lottery.users.repository.UserJdbcRepository;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.AuthService;
import com.team.lottery.users.service.TokenService;
import com.team.lottery.common.health.HealthController;
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
        startWith(cfg);
        log.info("Application started on port {}", cfg.port());
    }

    /**
     * Запуск приложения с заданным портом — переопределяет порт в конфиге,
     * остальное берёт из application.properties / ENV.
     * Используется в тестах, где нужен изолированный порт.
     */
    public static Javalin start(int port) {
        AppConfig cfg = AppConfig.load();
        AppConfig cfgWithPort = new AppConfig(
                port,
                cfg.dbUrl(),
                cfg.dbUser(),
                cfg.dbPassword(),
                cfg.dbPoolSize(),
                cfg.bcryptCost(),
                cfg.drawSchedulerIntervalSeconds(),
                cfg.isProd()
        );
        return startWith(cfgWithPort);
    }

    /**
     * Единственная точка сборки приложения. Принимает уже готовый AppConfig
     * (с любыми переопределениями — порт, URL БД и т.п.). Используется как
     * напрямую (из тестов с кастомным DataSource), так и через main / start(int).
     */
    public static Javalin startWith(AppConfig cfg) {
        DataSource ds = DatabaseConfig.init(cfg);

        HealthController healthController = new HealthController(ds);

        UserRepository userRepository = new UserJdbcRepository(ds);
        TokenService tokenService = new TokenService();
        AuthService authService = new AuthService(tokenService, userRepository);

        AuthController authController = new AuthController(userRepository, tokenService, authService);
        UserController userController = new UserController(userRepository, tokenService, authService);

        DrawRepository drawRepository = new DrawJdbcRepository(ds);
        DrawResultRepository drawResultRepository = new DrawResultJdbcRepository(ds);

        TicketRepository ticketRepository = new TicketJdbcRepository(ds);
        TicketService ticketService = new TicketService(ds, ticketRepository, drawRepository);
        TicketController ticketController = new TicketController(ticketService, authService);

        DrawService drawService = new DrawService(ds, drawRepository, drawResultRepository, ticketRepository);
        DrawController drawController = new DrawController(drawService);
        AdminDrawController adminDrawController = new AdminDrawController(drawService, authService);

        DrawScheduler drawScheduler = new DrawScheduler(drawRepository, drawService);

        Javalin app = JavalinConfig.create(
                cfg.isProd(),
                cfg.port(),
                routes -> {
                    routes.get("/", ctx -> ctx.result("Server is running"));
                    healthController.registerRoutes(routes);
                    authController.registerRoutes(routes);
                    userController.registerRoutes(routes);
                    drawController.registerRoutes(routes);
                    adminDrawController.registerRoutes(routes);
                    ticketController.registerRoutes(routes);
                }
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            drawScheduler.stop();
            app.stop();
            if (ds instanceof HikariDataSource hds) {
                hds.close();
            }
        }, "app-shutdown"));

        drawScheduler.start();
        app.start();

        return app;
    }
}
