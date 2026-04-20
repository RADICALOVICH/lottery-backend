package com.team.lottery.users.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import java.util.ArrayList;
import java.util.List;

public class TokenService {

    private final Map<String, Long> tokenToUserId = new ConcurrentHashMap<>();
    private final Map<Long, String> userIdToToken = new ConcurrentHashMap<>();

    public String generateOrGetToken(long userId) {
        String existingToken = userIdToToken.get(userId);

        if (existingToken != null) {
            return existingToken;
        }

        String newToken = UUID.randomUUID().toString();
        tokenToUserId.put(newToken, userId);
        userIdToToken.put(userId, newToken);

        return newToken;
    }

    public Long getUserIdByToken(String token) {
        return tokenToUserId.get(token);
    }

    public boolean hasToken(long userId) {
        return userIdToToken.containsKey(userId);
    }

    public String getTokenByUserId(long userId) {
        return userIdToToken.get(userId);
    }

    public void removeToken(String token) {
        Long userId = tokenToUserId.remove(token);

        if (userId != null) {
            userIdToToken.remove(userId);
        }
    }
    public List<Long> getLoggedInUserIds() {
        return new ArrayList<>(userIdToToken.keySet());
    }

}