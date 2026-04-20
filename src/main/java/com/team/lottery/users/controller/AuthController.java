package com.team.lottery.users.controller;

import com.team.lottery.common.errors.ConflictException;
import com.team.lottery.common.errors.UnauthorizedException;
import com.team.lottery.common.errors.ValidationException;
import com.team.lottery.users.dto.LoginRequest;
import com.team.lottery.users.dto.RegisterRequest;
import com.team.lottery.users.model.UserAuthData;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.TokenService;
import com.team.lottery.users.util.AuthUtil;
import com.team.lottery.users.util.AuthValidationUtil;
import com.team.lottery.users.util.PasswordUtil;
import io.javalin.http.Context;

import io.javalin.config.RoutesConfig;

import java.util.Map;

public class AuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AuthController(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public void register(Context ctx) throws Exception {
        RegisterRequest request = ctx.bodyAsClass(RegisterRequest.class);

        String loginError = AuthValidationUtil.validateLogin(request.getLogin());
        if (loginError != null) {
            throw new ValidationException(loginError);
        }

        String passwordError = AuthValidationUtil.validatePassword(request.getPassword());
        if (passwordError != null) {
            throw new ValidationException(passwordError);
        }

        String login = request.getLogin().trim();

        if (userRepository.existsByLogin(login)) {
            throw new ConflictException("Login already exists");
        }

        String passwordHash = PasswordUtil.hashPassword(request.getPassword());
        long userId = userRepository.createUser(login, passwordHash);

        ctx.status(201).json(Map.of(
                "id", userId,
                "login", login,
                "message", "User registered successfully"
        ));
    }

    public void login(Context ctx) throws Exception {
        LoginRequest request = ctx.bodyAsClass(LoginRequest.class);

        String loginError = AuthValidationUtil.validateLogin(request.getLogin());
        if (loginError != null) {
            throw new ValidationException(loginError);
        }

        String passwordError = AuthValidationUtil.validatePassword(request.getPassword());
        if (passwordError != null) {
            throw new ValidationException(passwordError);
        }

        String login = request.getLogin().trim();

        UserAuthData user = userRepository.findByLogin(login);
        if (user == null) {
            throw new UnauthorizedException("Invalid login or password");
        }

        boolean passwordMatches = PasswordUtil.matches(
                request.getPassword(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new UnauthorizedException("Invalid login or password");
        }

        if (tokenService.hasToken(user.getId())) {
            String existingToken = tokenService.getTokenByUserId(user.getId());

            ctx.status(200).json(Map.of(
                    "message", "User is already logged in",
                    "token", existingToken,
                    "id", user.getId(),
                    "login", user.getLogin(),
                    "role", user.getRole()
            ));
            return;
        }

        String token = tokenService.generateOrGetToken(user.getId());

        ctx.status(200).json(Map.of(
                "message", "Login successful",
                "token", token,
                "id", user.getId(),
                "login", user.getLogin(),
                "role", user.getRole()
        ));
    }

    public void logout(Context ctx) throws Exception {
        String token = AuthUtil.extractToken(ctx);
        AuthUtil.requireUserByToken(token, tokenService, userRepository);

        tokenService.removeToken(token);

        ctx.status(200).json(Map.of(
                "message", "Logout successful"
        ));
    }
    public void registerRoutes(RoutesConfig routes) {
        routes.post("/auth/register", this::register);
        routes.post("/auth/login", this::login);
        routes.post("/auth/logout", this::logout);
    }
}