package com.team.lottery.draws.dto;

import com.team.lottery.draws.model.DrawStatus;

import java.time.OffsetDateTime;

public class DrawResponse {
    private Long id;
    private String title;
    private DrawStatus status;
    private OffsetDateTime endDate;
    private Integer totalTickets;

    public DrawResponse() {
    }

    public DrawResponse(Long id,
                        String title,
                        DrawStatus status,
                        OffsetDateTime endDate,
                        Integer totalTickets) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.endDate = endDate;
        this.totalTickets = totalTickets;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DrawStatus getStatus() {
        return status;
    }

    public void setStatus(DrawStatus status) {
        this.status = status;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(Integer totalTickets) {
        this.totalTickets = totalTickets;
    }
}
