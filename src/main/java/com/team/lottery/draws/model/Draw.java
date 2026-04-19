package com.team.lottery.draws.model;

import java.time.OffsetDateTime;

public class Draw {
    private Long id;
    private String title;
    private DrawStatus status;
    private OffsetDateTime endDate;
    private Integer totalTickets;
    private Long createdBy;
    private OffsetDateTime createdAt;

    public Draw() {
    }

    public Draw(Long id,
                String title,
                DrawStatus status,
                OffsetDateTime endDate,
                Integer totalTickets,
                Long createdBy,
                OffsetDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.endDate = endDate;
        this.totalTickets = totalTickets;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
