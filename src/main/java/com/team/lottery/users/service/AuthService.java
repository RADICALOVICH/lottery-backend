package com.team.lottery.users.service;

import com.team.lottery.common.errors.ForbiddenException;
import com.team.lottery.common.errors.UnauthorizedException;
import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.repository.UserRepository;
import io.javalin.http.Context;

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

        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
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