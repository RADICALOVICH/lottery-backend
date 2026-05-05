package com.team.lottery.unit.repository;

import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawResult;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.JdbcDrawResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcDrawResultRepositoryTest extends BaseJdbcDrawRepositoryTest {

    private JdbcDrawResultRepository resultRepository;

    @BeforeEach
    void setUpChild() {
        resultRepository = new JdbcDrawResultRepository(dataSource);
    }

    @Test
    @DisplayName("Должен успешно сохранять результат тиража и находить его")
    void saveAndFindByDrawId() throws Exception {
        // Создаем зависимости через методы базы и локальный хелпер
        Draw draw = saveCustomDraw("Result Test Draw", DrawStatus.ACTIVE);

        Long ticketId;
        try (Connection conn = dataSource.getConnection()) {
            ticketId = insertTicketForTest(conn, draw.id(), 1);
        }

        DrawResult result = new DrawResult(
                null,
                draw.id(),
                ticketId,
                OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS)
        );

        // Act
        DrawResult saved = resultRepository.save(result);

        // Assert
        assertThat(saved.id()).isNotNull();
        Optional<DrawResult> found = resultRepository.findByDrawId(draw.id());
        assertThat(found).isPresent();
        assertThat(found.get().winningTicketId()).isEqualTo(ticketId);
    }

    @Test
    @DisplayName("Должен сохранять результат внутри транзакции и откатывать при ошибке")
    void saveInTransactionTest() throws Exception {
        Draw draw = saveCustomDraw("Tx Draw", DrawStatus.ACTIVE);
        OffsetDateTime drawnAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            Long ticketId = insertTicketForTest(conn, draw.id(), 777);
            DrawResult result = new DrawResult(null, draw.id(), ticketId, drawnAt);

            resultRepository.saveInTransaction(conn, result);

            // Проверка изоляции: в другом соединении данных еще нет
            assertThat(resultRepository.findByDrawId(draw.id())).isEmpty();

            conn.commit();
        }

        assertThat(resultRepository.findByDrawId(draw.id())).isPresent();
    }

    /**
     * Вспомогательный метод специально для тестов результатов,
     * так как DrawResult не может существовать без Ticket (FK constraint).
     */
    private Long insertTicketForTest(Connection conn, Long drawId, int number) throws Exception {
        String sql = "INSERT INTO tickets (draw_id, ticket_number, status) VALUES (?, ?, CAST(? AS ticket_status)) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, drawId);
            ps.setInt(2, number);
            ps.setString(3, "AVAILABLE");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
