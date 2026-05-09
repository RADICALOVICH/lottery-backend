package com.team.lottery.ticket.repository;

import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class TicketJdbcRepository implements TicketRepository {
    private final DataSource dataSource;

    public TicketJdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Ticket> findById(long id) {
        String sql = """
                SELECT id, draw_id, owner_id, ticket_number, status, created_at
                FROM tickets
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error in findById: " + id, e);
        }
    }

    @Override
    public List<Ticket> findByOwnerId(long ownerId) {
        String sql = """
                SELECT id, draw_id, owner_id, ticket_number, status, created_at
                FROM tickets
                WHERE owner_id = ?
                ORDER BY id
                """;
        return executeQuery(sql, ps -> ps.setLong(1, ownerId));
    }

    @Override
    public List<Ticket> findByDrawId(long drawId) {
        String sql = """
                SELECT id, draw_id, owner_id, ticket_number, status, created_at
                FROM tickets
                WHERE draw_id = ?
                ORDER BY ticket_number
                """;
        return executeQuery(sql, ps -> ps.setLong(1, drawId));
    }

    @Override
    public Optional<Ticket> findAnyAvailableByDrawIdForUpdate(Connection connection, long drawId) {
        String sql = """
                SELECT id, draw_id, owner_id, ticket_number, status, created_at
                FROM tickets
                WHERE draw_id = ? AND status = 'AVAILABLE'
                ORDER BY ticket_number
                LIMIT 1
                FOR UPDATE SKIP LOCKED
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, drawId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Locking error for draw: " + drawId, e);
        }
    }

    @Override
    public boolean buyTicket(Connection connection, long ticketId, long userId) {
        String sql = """
                UPDATE tickets
                SET owner_id = ?, status = 'SOLD'
                WHERE id = ? AND status = 'AVAILABLE' AND owner_id IS NULL
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, ticketId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Update error in buyTicket for ID: " + ticketId, e);
        }
    }

    @Override
    public void updateStatus(Connection connection, long ticketId, TicketStatus status) {
        String sql = """
                UPDATE tickets
                SET status = ?::ticket_status
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update status error for ticket ID: " + ticketId, e);
        }
    }

    @Override
    public void updateStatusesByDrawIdAndCurrentStatus(Connection connection, long drawId, TicketStatus currentStatus, TicketStatus newStatus) {
        String sql = """
                UPDATE tickets
                SET status = ?::ticket_status
                WHERE draw_id = ? AND status = ?::ticket_status
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setLong(2, drawId);
            ps.setString(3, currentStatus.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Batch update error for draw: " + drawId, e);
        }
    }

    @Override
    public void createTickets(long drawId, int totalTickets) {
        String sql = """
                INSERT INTO tickets (draw_id, owner_id, ticket_number, status)
                VALUES (?, NULL, ?, 'AVAILABLE')
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false); // Для ускорения пакетной вставки
            for (int i = 1; i <= totalTickets; i++) {
                ps.setLong(1, drawId);
                ps.setInt(2, i);
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to batch create tickets for draw: " + drawId, e);
        }
    }

    @Override
    public Ticket save(Connection connection, Ticket ticket) {
        String sql = """
                INSERT INTO tickets (draw_id, owner_id, ticket_number, status, created_at)
                VALUES (?, ?, ?, ?::ticket_status, ?)
                RETURNING id, draw_id, owner_id, ticket_number, status, created_at
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, ticket.drawId());
            if (ticket.ownerId() == null) ps.setNull(2, Types.BIGINT);
            else ps.setLong(2, ticket.ownerId());
            ps.setInt(3, ticket.ticketNumber());
            ps.setString(4, ticket.status().name());
            ps.setTimestamp(5, Timestamp.from(ticket.createdAt()));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RuntimeException("Insert failed");
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save ticket", e);
        }
    }

    // Вспомогательный интерфейс для лямбд в запросах
    @FunctionalInterface
    private interface PreparedStatementSetter {
        void setValues(PreparedStatement ps) throws SQLException;
    }

    private List<Ticket> executeQuery(String sql, PreparedStatementSetter setter) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.setValues(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Ticket> tickets = new ArrayList<>();
                while (rs.next()) {
                    tickets.add(mapRow(rs));
                }
                return tickets;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed", e);
        }
    }

    private Ticket mapRow(ResultSet rs) throws SQLException {
        Object owner = rs.getObject("owner_id");
        Long ownerId = owner == null ? null : ((Number) owner).longValue();

        String rawStatus = rs.getString("status");
        if (rawStatus == null) {
            throw new IllegalStateException("Database integrity error: Ticket ID " +
                    rs.getLong("id") + " has a NULL status in the database.");
        }

        try {
            TicketStatus status = TicketStatus.valueOf(rawStatus.trim().toUpperCase());
            return new Ticket(
                    rs.getLong("id"),
                    rs.getLong("draw_id"),
                    ownerId,
                    rs.getInt("ticket_number"),
                    status,
                    rs.getTimestamp("created_at").toInstant()
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown status value in database: [" + rawStatus +
                    "] for ticket ID: " + rs.getLong("id"), e);
        }
    }
}
