package com.team.lottery.users.controller;

import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.TokenService;
import com.team.lottery.users.util.AuthUtil;
import io.javalin.http.Context;
import io.javalin.config.RoutesConfig;

import java.util.Map;

public class UserController {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public UserController(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public void me(Context ctx) throws Exception {
        UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);

        ctx.status(200).json(Map.of(
                "id", currentUser.getId(),
                "login", currentUser.getLogin(),
                "role", currentUser.getRole()
        ));
    }

    public void findAll(Context ctx) throws Exception {
        UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);
        AuthUtil.requireAdmin(currentUser);

        ctx.status(200).json(userRepository.findAllUsers());
    }

    public void adminPing(Context ctx) throws Exception {
        UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);
        AuthUtil.requireAdmin(currentUser);

        ctx.json(Map.of(
                "message", "Admin access granted",
                "login", currentUser.getLogin(),
                "role", currentUser.getRole()
        ));
    }

    public void findLoggedInUsers(Context ctx) throws Exception {
        UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);
        AuthUtil.requireAdmin(currentUser);

        var loggedInUserIds = tokenService.getLoggedInUserIds();
        var users = userRepository.findUsersByIds(loggedInUserIds);

        ctx.status(200).json(users);
    }

    public void registerRoutes(RoutesConfig routes) {
        routes.get("/users/me", this::me);
        routes.get("/users", this::findAll);
        routes.get("/admin/ping", this::adminPing);
        routes.get("/admin/logged-in-users", this::findLoggedInUsers);
    }
}