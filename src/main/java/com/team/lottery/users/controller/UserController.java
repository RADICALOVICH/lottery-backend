package com.team.lottery.users.controller;

import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.AuthService;
import com.team.lottery.users.service.TokenService;
import io.javalin.http.Context;
import io.javalin.config.RoutesConfig;

import java.util.Map;

public class UserController {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthService auth;

    public UserController(UserRepository userRepository, TokenService tokenService, AuthService auth) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.auth = auth;
    }

    public void registerRoutes(RoutesConfig routes) {
        routes.get("/users/me", ctx -> {
            UserResponse currentUser = auth.requireUser(ctx);

            ctx.status(200).json(Map.of(
                    "id", currentUser.getId(),
                    "login", currentUser.getLogin(),
                    "role", currentUser.getRole()
            ));
        });

        routes.get("/users", ctx -> {
            auth.requireAdmin(ctx);
            ctx.status(200).json(userRepository.findAllUsers());
        });

        routes.get("/admin/ping", ctx -> {
            UserResponse currentUser = auth.requireAdmin(ctx);

            ctx.json(Map.of(
                    "message", "Admin access granted",
                    "login", currentUser.getLogin(),
                    "role", currentUser.getRole()
            ));
        });

        routes.get("/admin/logged-in-users", ctx -> {
            auth.requireAdmin(ctx);

            var loggedInUserIds = tokenService.getLoggedInUserIds();
            var users = userRepository.findUsersByIds(loggedInUserIds);

            ctx.status(200).json(users);
        });
    }
}