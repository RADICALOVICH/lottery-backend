package com.team.lottery.draws.model;

import java.time.OffsetDateTime;

public record Draw(
        Long id,
        String title,
        DrawStatus status,
        OffsetDateTime endDate,
        Integer totalTickets,
        Long createdBy,
        OffsetDateTime createdAt
) {
}
