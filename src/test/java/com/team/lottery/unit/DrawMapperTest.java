package com.team.lottery.unit;

import com.team.lottery.draws.dto.DrawResponse;
import com.team.lottery.draws.dto.DrawResultResponse;
import com.team.lottery.draws.mapper.DrawMapper;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawResult;
import com.team.lottery.draws.model.DrawStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DrawMapperTest {

    @Test
    void shouldMapDrawToResponse() {
        /*
        Должен корректно маппить Draw в DrawResponse.
        * */

        Long id = 1l;
        String title = "Весенний розыгрыш";
        DrawStatus status = DrawStatus.ACTIVE;
        OffsetDateTime endDate = OffsetDateTime.now().plusDays(7);
        int totalTickets = 500;

        Draw draw = new Draw(id, title, status, endDate, totalTickets, null, null);

        DrawResponse response = DrawMapper.toResponse(draw);


        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.title()).isEqualTo(title);
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.endDate()).isEqualTo(endDate);
        assertThat(response.totalTickets()).isEqualTo(totalTickets);
    }

    @Test

    void shouldMapDrawResultToResultResponse() {
        /*
        Должен корректно маппить DrawResult в DrawResultResponse.
        * */

        Long drawId = 1L;
        Long winningTicketId = 1L;
        OffsetDateTime drawnAt = OffsetDateTime.now();

        DrawResult result = new DrawResult();
        result.setDrawId(drawId);
        result.setWinningTicketId(winningTicketId);
        result.setDrawnAt(drawnAt);


        DrawResultResponse response = DrawMapper.toResultResponse(result);


        assertThat(response).isNotNull();
        assertThat(response.drawId()).isEqualTo(drawId);
        assertThat(response.winningTicketId()).isEqualTo(winningTicketId);
        assertThat(response.drawnAt()).isEqualTo(drawnAt);
    }
}
