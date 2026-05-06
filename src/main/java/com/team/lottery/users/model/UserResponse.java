package com.team.lottery.users.model;

public record UserResponse(
        long id,
        String login,
        String role
) {
}
