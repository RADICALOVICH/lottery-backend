package com.team.lottery.draws.model;

import java.time.OffsetDateTime;

public class DrawResult {
    private Long id;
    private Long drawId;
    private Long winningTicketId;
    private OffsetDateTime drawnAt;

    public DrawResult() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
