package com.team.lottery.users.model;


public class UserResponse {
    private final long id;
    private final String login;
    private final String role;

    public UserResponse(long id, String login, String role) {
        this.id = id;
        this.login = login;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getRole() {
        return role;
    }
}