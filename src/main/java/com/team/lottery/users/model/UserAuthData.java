package com.team.lottery.users.model;

public class UserAuthData {
    private final long id;
    private final String login;
    private final String passwordHash;
    private final String role;

    public UserAuthData(long id, String login, String passwordHash, String role) {
        this.id = id;
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }
}