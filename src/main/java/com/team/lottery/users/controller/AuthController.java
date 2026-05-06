package com.team.lottery.users.controller;

import com.team.lottery.common.errors.ConflictException;
import com.team.lottery.common.errors.UnauthorizedException;
import com.team.lottery.users.dto.LoginRequest;
import com.team.lottery.users.dto.RegisterRequest;
import com.team.lottery.users.model.UserAuthData;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.AuthService;
import com.team.lottery.users.service.TokenService;
import com.team.lottery.users.util.PasswordUtil;
import com.team.lottery.users.validation.AuthValidators;
import io.javalin.config.RoutesConfig;

import java.util.Map;

public class AuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthService auth;

    public AuthController(UserRepository userRepository, TokenService tokenService, AuthService auth) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.auth = auth;
    }

    public void registerRoutes(RoutesConfig routes) {
        routes.post("/auth/register", ctx -> {
            RegisterRequest request = ctx.bodyAsClass(RegisterRequest.class);

            AuthValidators.login(request.login());
            AuthValidators.password(request.password());

            String login = request.login().trim();

            if (userRepository.existsByLogin(login)) {
                throw new ConflictException("Login already exists");
            }

            String passwordHash = PasswordUtil.hashPassword(request.password());
            long userId = userRepository.createUser(login, passwordHash);

            ctx.status(201).json(Map.of(
                    "id", userId,
                    "login", login,
                    "message", "User registered successfully"
            ));
        });

        routes.post("/auth/login", ctx -> {
            String successMsg = "User is already logged in";
            String failureMsg = "Invalid login or password";
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);

            AuthValidators.login(request.login());
            AuthValidators.password(request.password());

            String login = request.login().trim();

            UserAuthData user = userRepository.findByLogin(login)
                    .orElseThrow(() -> new UnauthorizedException(failureMsg));
            if (user == null) {
                throw new UnauthorizedException(failureMsg);
            }

            boolean passwordMatches = PasswordUtil.matches(
                    request.password(),
                    user.passwordHash()
            );

            if (!passwordMatches) {
                throw new UnauthorizedException(failureMsg);
            }

            if (tokenService.hasToken(user.id())) {
                String existingToken = tokenService.getTokenByUserId(user.id());

                ctx.status(200).json(Map.of(
                        "message", successMsg,
                        "token", existingToken,
                        "id", user.id(),
                        "login", user.login(),
                        "role", user.role()
                ));
                return;
            }

            String token = tokenService.generateOrGetToken(user.id());

            ctx.status(200).json(Map.of(
                    "message", successMsg,
                    "token", token,
                    "id", user.id(),
                    "login", user.login(),
                    "role", user.role()
            ));
        });

        routes.post("/auth/logout", ctx -> {
            auth.logout(ctx);
            ctx.status(200).json(Map.of("message", "Logout successful"));
        });
    }
}