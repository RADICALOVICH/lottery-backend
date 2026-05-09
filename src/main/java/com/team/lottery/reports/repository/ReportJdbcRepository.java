package com.team.lottery.reports.repository;

import com.team.lottery.reports.dto.DrawReportEntry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReportJdbcRepository implements ReportRepository {
    private final DataSource dataSource;

    public ReportJdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<DrawReportEntry> getCompletedDrawsReport() {
        String sql = """
                SELECT
                    d.id                AS draw_id,
                    d.title             AS title,
                    d.created_at        AS created_at,
                    d.end_date          AS end_date,
                    d.total_tickets     AS total_tickets,
                    sold.count          AS sold_tickets,
                    wt.ticket_number    AS winner_ticket_number,
                    wt.owner_id         AS winner_user_id,
                    wu.login            AS winner_login,
                    dr.drawn_at         AS drawn_at,
                    d.created_by        AS created_by_admin_id,
                    ca.login            AS created_by_admin_login
                FROM draws d
                INNER JOIN draw_results dr ON dr.draw_id = d.id
                INNER JOIN tickets wt      ON wt.id = dr.winning_ticket_id
                LEFT  JOIN users wu        ON wu.id = wt.owner_id
                LEFT  JOIN users ca        ON ca.id = d.created_by
                INNER JOIN (
                    SELECT draw_id, count(*) AS count
                    FROM tickets
                    WHERE owner_id IS NOT NULL
                    GROUP BY draw_id
                ) sold ON sold.draw_id = d.id
                WHERE d.status = CAST(? AS draw_status)
                ORDER BY dr.drawn_at DESC
                """;

        List<DrawReportEntry> entries = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "COMPLETED");

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(mapEntry(resultSet));
                }
            }
            return entries;
        } catch (SQLException e) {
            throw new RuntimeException("Database error while building completed draws report", e);
        }
    }

    private DrawReportEntry mapEntry(ResultSet rs) throws SQLException {
        return new DrawReportEntry(
                rs.getLong("draw_id"),
                rs.getString("title"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("end_date", OffsetDateTime.class),
                rs.getInt("total_tickets"),
                rs.getInt("sold_tickets"),
                rs.getInt("winner_ticket_number"),
                rs.getObject("winner_user_id", Long.class),
                rs.getString("winner_login"),
                rs.getObject("drawn_at", OffsetDateTime.class),
                rs.getObject("created_by_admin_id", Long.class),
                rs.getString("created_by_admin_login")
        );
    }
}
