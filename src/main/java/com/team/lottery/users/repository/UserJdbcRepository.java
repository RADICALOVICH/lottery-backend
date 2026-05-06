package com.team.lottery.users.repository;

import com.team.lottery.users.model.UserAuthData;
import com.team.lottery.users.model.UserResponse;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserJdbcRepository implements UserRepository {

    private final DataSource ds;

    public UserJdbcRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public boolean existsByLogin(String login) {
        String sql = "SELECT 1 FROM users WHERE login = ? LIMIT 1";

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while checking login existence", e);
        }
    }

    @Override
    public long createUser(String login, String passwordHash) {
        String sql = """
                INSERT INTO users (login, password_hash, role)
                VALUES (?, ?, 'USER')
                RETURNING id
                """;

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);
            statement.setString(2, passwordHash);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
                throw new SQLException("Failed to insert user");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while creating user", e);
        }
    }

    @Override
    public Optional<UserAuthData> findByLogin(String login) {
        String sql = """
                SELECT id, login, password_hash, role
                FROM users
                WHERE login = ?
                LIMIT 1
                """;

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new UserAuthData(
                            resultSet.getLong("id"),
                            resultSet.getString("login"),
                            resultSet.getString("password_hash"),
                            resultSet.getString("role")
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding user by login", e);
        }
    }

    @Override
    public Optional<UserResponse> findById(long id) {
        String sql = """
                SELECT id, login, role
                FROM users
                WHERE id = ?
                LIMIT 1
                """;

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new UserResponse(
                            resultSet.getLong("id"),
                            resultSet.getString("login"),
                            resultSet.getString("role")
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding user by id", e);
        }
    }

    @Override
    public List<UserResponse> findAllUsers() {
        String sql = """
                SELECT id, login, role
                FROM users
                ORDER BY id
                """;

        List<UserResponse> users = new ArrayList<>();

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(new UserResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("login"),
                        resultSet.getString("role")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding all users", e);
        }

        return users;
    }

    @Override
    public List<UserResponse> findUsersByIds(List<Long> ids) {
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

        try (Connection connection = ds.getConnection();
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
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding users by ids", e);
        }

        return users;
    }
}
