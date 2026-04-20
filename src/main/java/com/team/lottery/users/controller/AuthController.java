package com.team.lottery.users.controller;

import io.javalin.http.Context;
import com.team.lottery.users.dto.LoginRequest;
import com.team.lottery.users.dto.RegisterRequest;
import com.team.lottery.users.model.UserAuthData;
import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.TokenService;
import com.team.lottery.users.util.AuthUtil;
import com.team.lottery.users.util.AuthValidationUtil;
import com.team.lottery.users.util.PasswordUtil;
//import com.team.lottery.users.repository.OperationHistoryRepository;

import java.util.Map;

public class AuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    //private final OperationHistoryRepository operationHistoryRepository; //additional feature

    public AuthController(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        //this.operationHistoryRepository = new OperationHistoryRepository();  //additional feature
    }

    public void register(Context ctx) {
        try {
            RegisterRequest request = ctx.bodyAsClass(RegisterRequest.class);

            String loginError = AuthValidationUtil.validateLogin(request.getLogin());
            if (loginError != null) {
                ctx.status(400).json(Map.of("error", loginError));
                return;
            }

            String passwordError = AuthValidationUtil.validatePassword(request.getPassword());
            if (passwordError != null) {
                ctx.status(400).json(Map.of("error", passwordError));
                return;
            }

            String login = request.getLogin().trim();

            if (userRepository.existsByLogin(login)) {
                ctx.status(409).json(Map.of(
                        "error", "Login already exists"
                ));
                return;
            }

            String passwordHash = PasswordUtil.hashPassword(request.getPassword());
            long userId = userRepository.createUser(login, passwordHash);

            //operationHistoryRepository.logRegister(userId, login);

            ctx.status(201).json(Map.of(
                    "id", userId,
                    "login", login,
                    "message", "User registered successfully"
            ));
        } catch (Exception e) {
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "details", e.getMessage()
            ));
        }
    }

    public void login(Context ctx) {
        try {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);

            String loginError = AuthValidationUtil.validateLogin(request.getLogin());
            if (loginError != null) {
                ctx.status(400).json(Map.of("error", loginError));
                return;
            }

            String passwordError = AuthValidationUtil.validatePassword(request.getPassword());
            if (passwordError != null) {
                ctx.status(400).json(Map.of("error", passwordError));
                return;
            }

            String login = request.getLogin().trim();

            UserAuthData user = userRepository.findByLogin(login);

            if (user == null) {
                //operationHistoryRepository.logLoginFailed(login, "user_not_found");  //additional feature

                ctx.status(401).json(Map.of(
                        "error", "Invalid login or password"
                ));
                return;
            }

            boolean passwordMatches = PasswordUtil.matches(
                    request.getPassword(),
                    user.getPasswordHash()
            );

            if (!passwordMatches) {
                //operationHistoryRepository.logLoginFailed(login, "invalid_password"); //additional feature

                ctx.status(401).json(Map.of(
                        "error", "Invalid login or password"
                ));
                return;
            }

            if (tokenService.hasToken(user.getId())) {
                String existingToken = tokenService.getTokenByUserId(user.getId());

                //operationHistoryRepository.logLoginAlreadyActive(user.getId(), user.getLogin());  //additional feature

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

            //operationHistoryRepository.logLoginSuccess(user.getId(), user.getLogin(), false);  //additional feature

            ctx.status(200).json(Map.of(
                    "message", "Login successful",
                    "token", token,
                    "id", user.getId(),
                    "login", user.getLogin(),
                    "role", user.getRole()
            ));
        } catch (Exception e) {
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "details", e.getMessage()
            ));
        }
    }
    public void logout(Context ctx) {
        try {
            String token = AuthUtil.extractToken(ctx);

            if (token == null) {
                return;
            }

            Long userId = tokenService.getUserIdByToken(token);

            if (userId == null) {
                ctx.status(401).json(Map.of(
                        "error", "Invalid or expired token"
                ));
                return;
            }

            //additional feature
            UserResponse user = userRepository.findById(userId);
            if (user == null) {
                ctx.status(401).json(Map.of(
                        "error", "User not found"
                ));
                return;
            }

            //operationHistoryRepository.logLogout(user.getId(), user.getLogin());  //additional feature


            tokenService.removeToken(token);

            ctx.status(200).json(Map.of(
                    "message", "Logout successful"
            ));
        } catch (Exception e) {
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "details", e.getMessage()
            ));
        }
    }
}