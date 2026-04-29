package com.team.lottery.users.service;

import com.team.lottery.common.errors.ForbiddenException;
import com.team.lottery.common.errors.UnauthorizedException;
import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.repository.UserRepository;
import io.javalin.http.Context;

import java.sql.SQLException;

public class AuthService {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public AuthService(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    public UserResponse requireUser(Context ctx) {
        String token = extractBearerToken(ctx);

        Long userId = tokenService.getUserIdByToken(token);
        if (userId == null) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        UserResponse user;
        try {
            user = userRepository.findById(userId);
        } catch (SQLException e) {
            // временно: UserRepository пока бросает checked SQLException.
            // на следующем шаге переведём его на unchecked + Optional, и этот try/catch уйдёт.
            throw new RuntimeException("Failed to load user", e);
        }

        if (user == null) {
            throw new UnauthorizedException("User not found");
        }
        return user;
    }

    public UserResponse requireAdmin(Context ctx) {
        UserResponse user = requireUser(ctx);
        if (!"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Access denied");
        }
        return user;
    }

    private static String extractBearerToken(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Authorization header is required");
        }
        if (!header.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid authorization format");
        }
        String token = header.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new UnauthorizedException("Token is required");
        }
        return token;
    }
}