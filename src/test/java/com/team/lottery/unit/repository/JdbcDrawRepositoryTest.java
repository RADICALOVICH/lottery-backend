package com.team.lottery.unit.repository;


import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class JdbcDrawRepositoryTest extends BaseJdbcDrawRepositoryTest {

    @Test
    @DisplayName("Успешное сохранение и поиск тиража по ID")
    void saveAndFindById() {
        Draw draw = new Draw(
                null,
                "Jackpot 777",
                null,
                OffsetDateTime.now().plusDays(7).truncatedTo(ChronoUnit.MICROS),
                1000,
                testUserId,
                null
        );

        Draw saved = repository.save(draw);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.title()).isEqualTo("Jackpot 777");
        assertThat(saved.status()).isEqualTo(DrawStatus.ACTIVE); // По умолчанию в БД

        Optional<Draw> found = repository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().title()).isEqualTo("Jackpot 777");
    }

    @Test
    @DisplayName("Поиск всех тиражей по статусу")
    void findByStatus() {
        saveCustomDraw("Active 1", DrawStatus.ACTIVE);
        saveCustomDraw("Closed 1", DrawStatus.CLOSED);
        saveCustomDraw("Active 2", DrawStatus.ACTIVE);

        List<Draw> activeDraws = repository.findByStatus(DrawStatus.ACTIVE);

        assertThat(activeDraws).hasSize(2);
        assertThat(activeDraws).extracting(Draw::title)
                .containsExactlyInAnyOrder("Active 1", "Active 2");
    }

    @Test
    @DisplayName("Поиск активных тиражей, дата окончания которых прошла")
    void findActiveEndedDraws() {
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Просрочен, статус ACTIVE -> должен быть найден
        Draw expired = saveCustomDraw("Expired", DrawStatus.ACTIVE);
        forceUpdateEndDate(expired.id(), now.minusMinutes(5));

        // 2. Будущий, статус ACTIVE -> не должен быть найден
        saveCustomDraw("Future", DrawStatus.ACTIVE);

        // 3. Просрочен, но статус CLOSED -> не должен быть найден
        Draw closed = saveCustomDraw("Closed", DrawStatus.CLOSED);
        forceUpdateEndDate(closed.id(), now.minusMinutes(5));

        List<Draw> result = repository.findActiveEndedDraws(now);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Expired");
    }

    @Test
    @DisplayName("Обновление статуса в транзакции")
    void updateStatusInTransaction() throws Exception {
        Draw draw = saveCustomDraw("Tx Test", DrawStatus.ACTIVE);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            repository.updateStatusInTransaction(conn, draw.id(), DrawStatus.COMPLETED);

            // В другом соединении статус все еще старый (изоляция транзакций)
            Draw drawBeforeCommit = repository.findById(draw.id()).orElseThrow();
            assertThat(drawBeforeCommit.status()).isEqualTo(DrawStatus.ACTIVE);

            conn.commit();
        }

        Draw drawAfterCommit = repository.findById(draw.id()).orElseThrow();
        assertThat(drawAfterCommit.status()).isEqualTo(DrawStatus.COMPLETED);
    }

}
