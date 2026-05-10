package com.team.lottery.users.controller;

import com.team.lottery.common.errors.ConflictException;
import com.team.lottery.common.errors.UnauthorizedException;
import com.team.lottery.users.dto.LoginRequest;
import com.team.lottery.users.dto.LoginResponse;
import com.team.lottery.users.dto.LogoutResponse;
import com.team.lottery.users.dto.RegisterRequest;
import com.team.lottery.users.dto.RegisterResponse;
import com.team.lottery.users.model.UserAuthData;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.AuthService;
import com.team.lottery.users.service.TokenService;
import com.team.lottery.users.util.PasswordUtil;
import com.team.lottery.users.validation.AuthValidators;
import io.javalin.config.RoutesConfig;

public class AuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthService auth;
    private final PasswordUtil passwordUtil;

    public AuthController(UserRepository userRepository,
                          TokenService tokenService,
                          AuthService auth,
                          PasswordUtil passwordUtil) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.auth = auth;
        this.passwordUtil = passwordUtil;
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

            String passwordHash = passwordUtil.hashPassword(request.password());
            long userId = userRepository.createUser(login, passwordHash);

            ctx.status(201).json(new RegisterResponse(
                    userId,
                    login,
                    "User registered successfully"
            ));
        });

        routes.post("/auth/login", ctx -> {
            String successMsg = "Login successful";
            String failureMsg = "Invalid login or password";
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);

            AuthValidators.login(request.login());
            AuthValidators.password(request.password());

            String login = request.login().trim();

            UserAuthData user = userRepository.findByLogin(login)
                    .orElseThrow(() -> new UnauthorizedException(failureMsg));

            boolean passwordMatches = passwordUtil.matches(
                    request.password(),
                    user.passwordHash()
            );

            if (!passwordMatches) {
                throw new UnauthorizedException(failureMsg);
            }

            String token = tokenService.hasToken(user.id())
                    ? tokenService.getTokenByUserId(user.id())
                    : tokenService.generateOrGetToken(user.id());

            ctx.status(200).json(new LoginResponse(
                    successMsg,
                    token,
                    user.id(),
                    user.login(),
                    user.role()
            ));
        });

        routes.post("/auth/logout", ctx -> {
            auth.logout(ctx);
            ctx.status(200).json(new LogoutResponse("Logout successful"));
        });
    }
}
