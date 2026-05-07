package com.team.lottery.reports.dto;

import java.time.OffsetDateTime;

/**
 * Одна строка отчёта по завершённым тиражам.
 *
 * Поля {@code winnerUserId} и {@code winnerLogin} могут быть null, если
 * победителем оказался непроданный билет (правило лотереи допускает это —
 * розыгрыш проводится среди всех билетов тиража).
 *
 */
public record DrawReportEntry(
        Long drawId,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime endDate,
        Integer totalTickets,
        Integer soldTickets,
        Integer winnerTicketNumber,
        Long winnerUserId,
        String winnerLogin,
        OffsetDateTime drawnAt,
        Long createdByAdminId,
        String createdByAdminLogin
) {
}