package com.team.lottery.draws.dto;

import java.time.OffsetDateTime;

public record CreateDrawRequest(
        String title,
        OffsetDateTime endDate,
        Integer totalTickets
) {
}
