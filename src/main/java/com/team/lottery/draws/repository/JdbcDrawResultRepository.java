package com.team.lottery.draws.repository;

import com.team.lottery.common.db.JdbcHelper;
import com.team.lottery.draws.model.DrawResult;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.Optional;

public class JdbcDrawResultRepository implements DrawResultRepository {
    private final DataSource ds;

    public JdbcDrawResultRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public DrawResult save(DrawResult drawResult) {
        return JdbcHelper.withConnection(ds, c -> {
            var inserted = JdbcHelper.query(
                    c,
                    "INSERT INTO draw_results (draw_id, winning_ticket_id, drawn_at) " +
                            "VALUES (?, ?, ?) " +
                            "RETURNING id, draw_id, winning_ticket_id, drawn_at",
                    ps -> {
                        ps.setLong(1, drawResult.getDrawId());
                        if (drawResult.getWinningTicketId() != null) {
                            ps.setLong(2, drawResult.getWinningTicketId());
                        } else {
                            ps.setNull(2, java.sql.Types.BIGINT);
                        }
                        ps.setObject(3, drawResult.getDrawnAt());
                    },
                    rs -> {
                        DrawResult saved = new DrawResult();
                        saved.setId(rs.getLong("id"));
                        saved.setDrawId(rs.getLong("draw_id"));

                        long winningTicketId = rs.getLong("winning_ticket_id");
                        if (!rs.wasNull()) {
                            saved.setWinningTicketId(winningTicketId);
                        }

                        saved.setDrawnAt(rs.getObject("drawn_at", OffsetDateTime.class));
                        return saved;
                    }
            );

            return inserted.get(0);
        });
    }

    @Override
    public Optional<DrawResult> findByDrawId(Long drawId) {
        return JdbcHelper.withConnection(ds, c -> {
            var rows = JdbcHelper.query(
                    c,
                    "SELECT id, draw_id, winning_ticket_id, drawn_at " +
                            "FROM draw_results WHERE draw_id = ?",
                    ps -> ps.setLong(1, drawId),
                    rs -> {
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
            );
            return rows.stream().findFirst();
        });
    }
}
