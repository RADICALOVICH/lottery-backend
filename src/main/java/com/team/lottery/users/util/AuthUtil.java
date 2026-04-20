package com.team.lottery.users.util;

import io.javalin.http.Context;
import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.TokenService;

import java.sql.SQLException;
import java.util.Map;

public final class AuthUtil {

    private AuthUtil() {
    }

    public static String extractToken(Context ctx) {
        String authHeader = ctx.header("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            ctx.status(401).json(Map.of(
                    "error", "Authorization header is required"
            ));
            return null;
        }

        if (!authHeader.startsWith("Bearer ")) {
            ctx.status(401).json(Map.of(
                    "error", "Invalid authorization format"
            ));
            return null;
        }

        String token = authHeader.substring("Bearer ".length()).trim();

        if (token.isBlank()) {
            ctx.status(401).json(Map.of(
                    "error", "Token is required"
            ));
            return null;
        }

        return token;
    }

    public static UserResponse requireUser(
            Context ctx,
            TokenService tokenService,
            UserRepository userRepository
    ) throws SQLException {
        String token = extractToken(ctx);

        if (token == null) {
            return null;
        }

        Long userId = tokenService.getUserIdByToken(token);

        if (userId == null) {
            ctx.status(401).json(Map.of(
                    "error", "Invalid or expired token"
            ));
            return null;
        }

        UserResponse user = userRepository.findById(userId);

        if (user == null) {
            ctx.status(401).json(Map.of(
                    "error", "User not found"
            ));
            return null;
        }

        return user;
    }

    public static boolean denyIfNotAdmin(Context ctx, UserResponse user) {
        if (!"ADMIN".equals(user.getRole())) {
            ctx.status(403).json(Map.of(
                    "error", "Access denied"
            ));
            return true;
        }

        return false;
    }
}