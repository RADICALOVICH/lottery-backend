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
        return  new DrawResponse(
                draw.getId(),
                draw.getTitle(),
                draw.getStatus(),
                draw.getEndDate(),
                draw.getTotalTickets()
        );
    }

    public static DrawResultResponse toResultResponse(DrawResult drawResult) {
        DrawResultResponse response = new DrawResultResponse();
        response.setDrawId(drawResult.getDrawId());
        response.setWinningTicketId(drawResult.getWinningTicketId());
        response.setDrawnAt(drawResult.getDrawnAt());
        return response;
    }
}
