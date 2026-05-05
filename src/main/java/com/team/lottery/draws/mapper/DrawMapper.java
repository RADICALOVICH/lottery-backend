package com.team.lottery.draws.mapper;

import com.team.lottery.draws.dto.DrawResponse;
import com.team.lottery.draws.dto.DrawResultResponse;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawResult;

public final class DrawMapper {

    private DrawMapper() {
    }

    /**
     * Преобразует доменную модель {@code Draw} в DTO ответа {@code DrawResponse} для отправки клиенту.
     * @param draw доменная модель тиража
     * @return DTO для ответа клиенту
     */

    public static DrawResponse toResponse(Draw draw) {
        return new DrawResponse(
                draw.id(),
                draw.title(),
                draw.status(),
                draw.endDate(),
                draw.totalTickets()
        );
    }

    public static DrawResultResponse toResultResponse(DrawResult drawResult) {
        return new DrawResultResponse(
                drawResult.getDrawId(),
                drawResult.getWinningTicketId(),
                drawResult.getDrawnAt()
        );
    }
}
