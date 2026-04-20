package com.team.lottery;

import com.team.lottery.common.db.ConnectionFactory;
import com.team.lottery.config.AppConfig;
import com.team.lottery.config.DatabaseConfig;
import com.team.lottery.config.JavalinConfig;
import com.team.lottery.draws.controller.AdminDrawController;
import com.team.lottery.draws.controller.DrawController;
import com.team.lottery.draws.repository.DrawRepository;
import com.team.lottery.draws.repository.InMemoryDrawRepository;
import com.team.lottery.draws.service.DrawService;
import com.team.lottery.users.controller.AuthController;
import com.team.lottery.users.controller.UserController;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.TokenService;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.team.lottery.draws.repository.DrawResultRepository;
import com.team.lottery.draws.repository.InMemoryDrawResultRepository;

import javax.sql.DataSource;
import java.util.Map;


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
        ConnectionFactory.init(ds); //for Vladimir K // TO DO :find a way to merge all database acaonnection

        // User repository initialization
        UserRepository userRepository = new UserRepository();
        TokenService tokenService = new TokenService();

        AuthController authController = new AuthController(userRepository, tokenService);
        UserController userController = new UserController(userRepository, tokenService);

        //Draw repository initialization
        DrawRepository drawRepository = new InMemoryDrawRepository();
        DrawResultRepository drawResultRepository = new InMemoryDrawResultRepository();
        DrawService drawService = new DrawService(drawRepository, drawResultRepository);
        DrawController drawController = new DrawController(drawService);
        AdminDrawController adminDrawController = new AdminDrawController(drawService);

        Javalin app = JavalinConfig.create(cfg.port(), routes -> {
            //additonal health routes  // TO DO put them in addiotnal file

            // health routes
            routes.get("/", ctx -> ctx.result("Server is running"));

            //routes.get("/health", ctx -> ctx.status(200).json(Map.of(
            //        "status", "UP",
            //        "service", "lottery-api"
            //)));

            routes.get("/health/db", ctx -> {
                try (var connection = ds.getConnection()) {
                    boolean valid = connection.isValid(2);

                    if (valid) {
                        ctx.status(200).json(Map.of(
                                "status", "UP",
                                "database", "CONNECTED"
                        ));
                    } else {
                        ctx.status(503).json(Map.of(
                                "status", "DOWN",
                                "database", "DISCONNECTED"
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

            // auth routes
            routes.post("/auth/register", authController::register);
            routes.post("/auth/login", authController::login);
            routes.post("/auth/logout", authController::logout);

            // user routes
            routes.get("/users/me", userController::me);
            routes.get("/users", userController::findAll);
            routes.get("/admin/ping", userController::adminPing);
            routes.get("/admin/logged-in-users", userController::findLoggedInUsers);




            // draw routes
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
