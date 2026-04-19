package com.team.lottery.draws.dto;

import java.time.OffsetDateTime;

public class DrawResultResponse {
    private Long drawId;
    private Long winningTicketId;
    private OffsetDateTime drawnAt;

    public DrawResultResponse() {
    }

    public Long getDrawId() {
        return drawId;
    }

    public void setDrawId(Long drawId) {
        this.drawId = drawId;
    }

    public Long getWinningTicketId() {
        return winningTicketId;
    }

    public void setWinningTicketId(Long winningTicketId) {
        this.winningTicketId = winningTicketId;
    }

    public OffsetDateTime getDrawnAt() {
        return drawnAt;
    }

    public void setDrawnAt(OffsetDateTime drawnAt) {
        this.drawnAt = drawnAt;
    }
}
