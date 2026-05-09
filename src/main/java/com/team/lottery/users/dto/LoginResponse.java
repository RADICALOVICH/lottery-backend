package com.team.lottery.users.dto;

public record LoginResponse(String message, String token, long id, String login, String role) {
}
