package com.team.lottery.ticket.repository;

import com.team.lottery.common.db.JdbcHelper;
import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
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

        return JdbcHelper.withConnection(dataSource, connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Ticket> findByOwnerId(long ownerId) {
        String sql = """
                SELECT id, draw_id, owner_id, ticket_number, status, created_at
                FROM tickets
                WHERE owner_id = ?
                ORDER BY id
                """;

        return JdbcHelper.withConnection(dataSource, connection ->
                JdbcHelper.query(connection, sql, ps -> ps.setLong(1, ownerId), this::mapRow)
        );
    }

    @Override
    public List<Ticket> findByDrawId(long drawId) {
        String sql = """
                SELECT id, draw_id, owner_id, ticket_number, status, created_at
                FROM tickets
                WHERE draw_id = ?
                ORDER BY ticket_number
                """;

        return JdbcHelper.withConnection(dataSource, connection ->
                JdbcHelper.query(connection, sql, ps -> ps.setLong(1, drawId), this::mapRow)
        );
    }

    @Override
    public List<Ticket> findSoldByDrawId(long drawId) {
        String sql = """
                SELECT id, draw_id, owner_id, ticket_number, status, created_at
                FROM tickets
                WHERE draw_id = ? AND status = 'SOLD'
                ORDER BY ticket_number
                """;

        return JdbcHelper.withConnection(dataSource, connection ->
                JdbcHelper.query(connection, sql, ps -> ps.setLong(1, drawId), this::mapRow)
        );
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
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
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
        }
    }

    @Override
    public void updateStatus(long ticketId, TicketStatus status) {
        String sql = """
                UPDATE tickets
                SET status = ?::ticket_status
                WHERE id = ?
                """;

        JdbcHelper.withConnection(dataSource, connection -> {
            JdbcHelper.update(connection, sql, ps -> {
                ps.setString(1, status.name());
                ps.setLong(2, ticketId);
            });
            return null;
        });
    }

    @Override
    public void updateStatusesByDrawIdAndCurrentStatus(long drawId, TicketStatus currentStatus, TicketStatus newStatus) {
        String sql = """
                UPDATE tickets
                SET status = ?::ticket_status
                WHERE draw_id = ? AND status = ?::ticket_status
                """;

        JdbcHelper.withConnection(dataSource, connection -> {
            JdbcHelper.update(connection, sql, ps -> {
                ps.setString(1, newStatus.name());
                ps.setLong(2, drawId);
                ps.setString(3, currentStatus.name());
            });
            return null;
        });
    }

    @Override
    public Ticket save(Ticket ticket) {
        String sql = """
                INSERT INTO tickets (draw_id, owner_id, ticket_number, status, created_at)
                VALUES (?, ?, ?, ?::ticket_status, ?)
                RETURNING id, draw_id, owner_id, ticket_number, status, created_at
                """;

        return JdbcHelper.withConnection(dataSource, connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, ticket.drawId());

                if (ticket.ownerId() == null) {
                    ps.setNull(2, Types.BIGINT);
                } else {
                    ps.setLong(2, ticket.ownerId());
                }

                ps.setInt(3, ticket.ticketNumber());
                ps.setString(4, ticket.status().name());
                ps.setTimestamp(5, Timestamp.from(ticket.createdAt()));

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new RuntimeException("Failed to insert ticket");
                    }
                    return mapRow(rs);
                }
            }
        });
    }

    private Ticket mapRow(ResultSet rs) throws SQLException {
        Object owner = rs.getObject("owner_id");
        Long ownerId = owner == null ? null : ((Number) owner).longValue();

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