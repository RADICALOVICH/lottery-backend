package com.team.lottery;

import com.team.lottery.config.AppConfig;
import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.config.JavalinConfig;
import com.team.lottery.draws.controller.AdminDrawController;
import com.team.lottery.draws.controller.DrawController;
import com.team.lottery.draws.repository.*;
import com.team.lottery.draws.scheduler.DrawScheduler;
import com.team.lottery.draws.service.DrawService;
import com.team.lottery.ticket.controller.TicketController;
import com.team.lottery.ticket.repository.TicketJdbcRepository;
import com.team.lottery.ticket.repository.TicketRepository;
import com.team.lottery.ticket.service.TicketService;
import com.team.lottery.users.controller.AuthController;
import com.team.lottery.users.controller.UserController;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.AuthService;
import com.team.lottery.users.service.TokenService;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.team.lottery.draws.repository.DrawResultRepository;
import com.team.lottery.common.health.HealthController;

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

        //local database initialization for Vladimir Kuryndin -not used
        //DataSource ds = LocalDatabaseConfig.initWithoutFlyway();

        DataSource ds = DatabaseConfig.init(cfg);
        //ConnectionFactory.init(ds); //for Vladimir K // TO DO :find a way to merge all database acaonnection

        //healthController
        HealthController healthController = new HealthController(ds);

        // User repository initialization
        UserRepository userRepository = new UserRepository(ds);
        TokenService tokenService = new TokenService();
        AuthService authService = new AuthService(tokenService, userRepository);

        AuthController authController = new AuthController(userRepository, tokenService);
        UserController userController = new UserController(userRepository, tokenService, authService);

        //Draw repository initialization
        DrawRepository drawRepository = new JdbcDrawRepository(ds);
        DrawResultRepository drawResultRepository = new JdbcDrawResultRepository(ds);

        // Ticket repository initialization
        TicketRepository ticketRepository = new TicketJdbcRepository(ds);
        TicketService ticketService = new TicketService(ds, ticketRepository, drawRepository);
        TicketController ticketController = new TicketController(ticketService, authService);

        DrawService drawService = new DrawService(ds, drawRepository, drawResultRepository, ticketRepository);
        DrawController drawController = new DrawController(drawService);
        AdminDrawController adminDrawController = new AdminDrawController(drawService, authService);

/*
        // Ticket repository initialization
        TicketRepository ticketRepository = new TicketJdbcRepository(ds);
        TicketService ticketService = new TicketService(ds, ticketRepository);
        TicketController ticketController = new TicketController(ticketService, tokenService);

        //Draw repository initialization
        DrawRepository drawRepository = new JdbcDrawRepository(ds);
        DrawResultRepository drawResultRepository = new JdbcDrawResultRepository(ds);
        DrawService drawService = new DrawService(ds, drawRepository, drawResultRepository, ticketRepository);
        DrawController drawController = new DrawController(drawService);
        AdminDrawController adminDrawController = new AdminDrawController(drawService, userRepository, tokenService);
*/

        DrawScheduler drawScheduler = new DrawScheduler(drawRepository, drawService);

        System.out.println("DEBUG: isProd = " + cfg.isProd()); // Продуктиваная ли среда?

        Javalin app = JavalinConfig.create(cfg.isProd(),
                cfg.port(),
                routes -> {

            // health routes
            routes.get("/", ctx -> ctx.result("Server is running"));
            healthController.registerRoutes(routes);

            // auth routes
            authController.registerRoutes(routes);

            // user routes
            userController.registerRoutes(routes);

            // draw routes
            drawController.registerRoutes(routes);
            adminDrawController.registerRoutes(routes);

            // ticket routes
            ticketController.registerRoutes(routes);
        });

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
        log.info("Application started on port {}", cfg.port());
    }
}