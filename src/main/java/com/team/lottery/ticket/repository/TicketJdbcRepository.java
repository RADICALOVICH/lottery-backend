package com.team.lottery.ticket.repository;

import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TicketJdbcRepository implements TicketRepository {

    private final DataSource dataSource;

    public TicketJdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Ticket> findAnyAvailableByDrawIdForUpdate(Connection connection, long drawId) {
        String sql = """
                SELECT id, draw_id, owner_id, status
                FROM tickets
                WHERE draw_id = ? AND status = 'AVAILABLE'
                ORDER BY id
                LIMIT 1
                FOR UPDATE
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, drawId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find available ticket for draw: " + drawId, e);
        }
    }

    @Override
    public boolean buyTicket(Connection connection, long ticketId, long userId) {
        String sql = """
                UPDATE tickets
                SET owner_id = ?, status = 'SOLD'
                WHERE id = ? AND status = 'AVAILABLE'
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, ticketId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to buy ticket: " + ticketId, e);
        }
    }

    @Override
    public Optional<Ticket> findById(long ticketId) {
        String sql = """
                SELECT id, draw_id, owner_id, status
                FROM tickets
                WHERE id = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, ticketId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find ticket by id: " + ticketId, e);
        }
    }

    @Override
    public List<Ticket> findByOwnerId(long userId) {
        String sql = """
                SELECT id, draw_id, owner_id, status
                FROM tickets
                WHERE owner_id = ?
                ORDER BY id
                """;

        List<Ticket> result = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find tickets by owner id: " + userId, e);
        }
    }

    @Override
    public void createTickets(long drawId, int totalTickets) {
        String sql = """
            INSERT INTO tickets (draw_id, owner_id, ticket_number, status)
            VALUES (?, NULL, ?, 'AVAILABLE')
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            for (int i = 1; i <= totalTickets; i++) {
                ps.setLong(1, drawId);
                ps.setInt(2, i);
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tickets for draw: " + drawId, e);
        }
    }

    private Ticket mapRow(ResultSet rs) throws SQLException {
        Long ownerId = rs.getObject("owner_id") == null ? null : rs.getLong("owner_id");

        return new Ticket(
                rs.getLong("id"),
                rs.getLong("draw_id"),
                ownerId,
                rs.getInt("ticket_number"),
                TicketStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}