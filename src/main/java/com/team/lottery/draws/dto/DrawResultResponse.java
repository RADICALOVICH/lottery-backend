package com.team.lottery.draws.dto;

import java.time.OffsetDateTime;

public record DrawResultResponse(
        Long drawId,
        Long winningTicketId,
        OffsetDateTime drawnAt
) {
}
