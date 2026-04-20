package com.team.lottery.users.repository;

import com.team.lottery.common.db.ConnectionFactory;
import com.team.lottery.users.model.UserAuthData;
import com.team.lottery.users.model.UserResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserRepository {

    public boolean existsByLogin(String login) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE login = ? LIMIT 1";

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public long createUser(String login, String passwordHash) throws SQLException {
        String sql = """
                INSERT INTO users (login, password_hash, role)
                VALUES (?, ?, 'USER')
                RETURNING id
                """;

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);
            statement.setString(2, passwordHash);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
                throw new SQLException("Failed to insert user");
            }
        }
    }

    public UserAuthData findByLogin(String login) throws SQLException {
        String sql = """
                SELECT id, login, password_hash, role
                FROM users
                WHERE login = ?
                LIMIT 1
                """;

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new UserAuthData(
                            resultSet.getLong("id"),
                            resultSet.getString("login"),
                            resultSet.getString("password_hash"),
                            resultSet.getString("role")
                    );
                }
                return null;
            }
        }
    }

    public UserResponse findById(long id) throws SQLException {
        String sql = """
            SELECT id, login, role
            FROM users
            WHERE id = ?
            LIMIT 1
            """;

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new UserResponse(
                            resultSet.getLong("id"),
                            resultSet.getString("login"),
                            resultSet.getString("role")
                    );
                }
                return null;
            }
        }
    }
    public List<UserResponse> findAllUsers() throws SQLException {
        String sql = """
            SELECT id, login, role
            FROM users
            ORDER BY id
            """;

        List<UserResponse> users = new ArrayList<>();

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(new UserResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("login"),
                        resultSet.getString("role")
                ));
            }
        }

        return users;
    }

    public List<UserResponse> findUsersByIds(List<Long> ids) throws SQLException {
        List<UserResponse> users = new ArrayList<>();

        if (ids == null || ids.isEmpty()) {
            return users;
        }

        String placeholders = ids.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = """
            SELECT id, login, role
            FROM users
            WHERE id IN (%s)
            ORDER BY id
            """.formatted(placeholders);

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < ids.size(); i++) {
                statement.setLong(i + 1, ids.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(new UserResponse(
                            resultSet.getLong("id"),
                            resultSet.getString("login"),
                            resultSet.getString("role")
                    ));
                }
            }
        }

        return users;
    }
}