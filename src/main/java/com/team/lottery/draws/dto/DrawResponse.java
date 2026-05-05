package com.team.lottery.draws.dto;

import com.team.lottery.draws.model.DrawStatus;

import java.time.OffsetDateTime;

public record DrawResponse(
        Long id,
        String title,
        DrawStatus status,
        OffsetDateTime endDate,
        Integer totalTickets
) {
}
