package com.team.lottery.users.model;

public record UserAuthData(
        long id,
        String login,
        String passwordHash,
        String role
) {
}
