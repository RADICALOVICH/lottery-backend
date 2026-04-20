package com.team.lottery.draws.repository;

import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class JdbcDrawRepository implements DrawRepository {
    private final DataSource ds;

    public JdbcDrawRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Draw save(Draw draw) {
        String sql = """
                INSERT INTO draws (title, end_date, total_tickets, created_by)
                VALUES (?, ?, ?, ?)
                RETURNING id, title, status, end_date, total_tickets, created_by, created_at
                """;

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, draw.getTitle());
            statement.setObject(2, draw.getEndDate());
            statement.setInt(3, draw.getTotalTickets());
            statement.setLong(4, draw.getCreatedBy());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapDraw(resultSet);
                }
                throw new SQLException("Failed to insert draw");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while saving draw", e);
        }
    }

    @Override
    public Optional<Draw> findById(Long id) {
        String sql = """
                SELECT id, title, status, end_date, total_tickets, created_by, created_at
                FROM draws
                WHERE id = ?
                """;

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapDraw(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding draw by id", e);
        }
    }

    @Override
    public List<Draw> findAll() {
        String sql = """
                SELECT id, title, status, end_date, total_tickets, created_by, created_at
                FROM draws
                ORDER BY id ASC
                """;

        List<Draw> draws = new ArrayList<>();

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                draws.add(mapDraw(resultSet));
            }
            return draws;
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding all draws", e);
        }
    }

    @Override
    public List<Draw> findByStatus(DrawStatus status) {
        String sql = """
                SELECT id, title, status, end_date, total_tickets, created_by, created_at
                FROM draws
                WHERE status = CAST(? AS draw_status)
                ORDER BY id ASC
                """;

        List<Draw> draws = new ArrayList<>();

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    draws.add(mapDraw(resultSet));
                }
            }
            return draws;
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding draws by status", e);
        }
    }

    @Override
    public void updateStatus(Long drawId, DrawStatus status) {
        String sql = """
                UPDATE draws
                SET status = ?
                WHERE id = ?
                """;

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.name());
            statement.setLong(2, drawId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while updating draw status", e);
        }
    }

    private Draw mapDraw(ResultSet rs) throws SQLException {
        Draw draw = new Draw();
        draw.setId(rs.getLong("id"));
        draw.setTitle(rs.getString("title"));
        draw.setStatus(DrawStatus.valueOf(rs.getString("status")));
        draw.setEndDate(rs.getObject("end_date", OffsetDateTime.class));
        draw.setTotalTickets(rs.getInt("total_tickets"));
        draw.setCreatedBy(rs.getLong("created_by"));
        draw.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return draw;
    }
}