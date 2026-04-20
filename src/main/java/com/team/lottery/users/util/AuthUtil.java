package com.team.lottery.users.util;

import com.team.lottery.common.errors.ForbiddenException;
import com.team.lottery.common.errors.UnauthorizedException;
import io.javalin.http.Context;
import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.TokenService;

import java.sql.SQLException;

public final class AuthUtil {

    private AuthUtil() {
    }

    public static String extractToken(Context ctx) {
        String authHeader = ctx.header("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            throw new UnauthorizedException("Authorization header is required");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid authorization format");
        }

        String token = authHeader.substring("Bearer ".length()).trim();

        if (token.isBlank()) {
            throw new UnauthorizedException("Token is required");
        }

        return token;
    }

    public static UserResponse requireUser(
            Context ctx,
            TokenService tokenService,
            UserRepository userRepository
    ) throws SQLException {
        String token = extractToken(ctx);

        Long userId = tokenService.getUserIdByToken(token);
        if (userId == null) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        UserResponse user = userRepository.findById(userId);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }

        return user;
    }

    public static void requireAdmin(UserResponse user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Access denied");
        }
    }
}