package com.team.lottery.draws.repository;

import com.team.lottery.draws.model.DrawResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Optional;

public class JdbcDrawResultRepository implements DrawResultRepository {

    private final DataSource ds;

    public JdbcDrawResultRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public DrawResult save(DrawResult drawResult) {
        String sql = """
                INSERT INTO draw_results (draw_id, winning_ticket_id, drawn_at)
                VALUES (?, ?, ?)
                RETURNING id, draw_id, winning_ticket_id, drawn_at
                """;

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, drawResult.getDrawId());

            if (drawResult.getWinningTicketId() != null) {
                statement.setLong(2, drawResult.getWinningTicketId());
            } else {
                statement.setNull(2, Types.BIGINT);
            }

            statement.setObject(3, drawResult.getDrawnAt());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapDrawResult(resultSet);
                }
                throw new SQLException("Failed to insert draw result");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while saving draw result", e);
        }
    }

    @Override
    public DrawResult saveInTransaction(Connection connection, DrawResult drawResult) {
        String sql = """
                INSERT INTO draw_results (draw_id, winning_ticket_id, drawn_at)
                VALUES (?, ?, ?)
                RETURNING id, draw_id, winning_ticket_id, drawn_at
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, drawResult.getDrawId());

            if (drawResult.getWinningTicketId() != null) {
                statement.setLong(2, drawResult.getWinningTicketId());
            } else {
                statement.setNull(2, Types.BIGINT);
            }

            statement.setObject(3, drawResult.getDrawnAt());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapDrawResult(resultSet);
                }
                throw new SQLException("Failed to insert draw result");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while saving draw result", e);
        }
    }

    @Override
    public Optional<DrawResult> findByDrawId(Long drawId) {
        String sql = """
                SELECT id, draw_id, winning_ticket_id, drawn_at
                FROM draw_results
                WHERE draw_id = ?
                """;

        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, drawId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapDrawResult(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding draw result by draw id", e);
        }
    }

    private DrawResult mapDrawResult(ResultSet rs) throws SQLException {
        DrawResult drawResult = new DrawResult();
        drawResult.setId(rs.getLong("id"));
        drawResult.setDrawId(rs.getLong("draw_id"));

        long winningTicketId = rs.getLong("winning_ticket_id");
        if (!rs.wasNull()) {
            drawResult.setWinningTicketId(winningTicketId);
        }

        drawResult.setDrawnAt(rs.getObject("drawn_at", OffsetDateTime.class));
        return drawResult;
    }
}