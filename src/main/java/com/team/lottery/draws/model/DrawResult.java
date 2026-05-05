package com.team.lottery.draws.model;

import java.time.OffsetDateTime;

public record DrawResult(
        Long id,
        Long drawId,
        Long winningTicketId,
        OffsetDateTime drawnAt
) {
}
