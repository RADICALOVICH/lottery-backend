package com.team.lottery.users.util;

import com.team.lottery.common.errors.ForbiddenException;
import com.team.lottery.common.errors.UnauthorizedException;
import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.TokenService;
import io.javalin.http.Context;

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
    ) {
        String token = extractToken(ctx);
        return requireUserByToken(token, tokenService, userRepository);
    }

    public static void requireAdmin(UserResponse user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Access denied");
        }
    }

    public static UserResponse requireUserByToken(
            String token,
            TokenService tokenService,
            UserRepository userRepository
    ) {
        Long userId = tokenService.getUserIdByToken(token);

        if (userId == null) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}