package com.team.lottery.draws.dto;




import com.team.lottery.common.errors.ConflictException;

import java.time.OffsetDateTime;
import java.util.Objects;


public class CreateDrawRequest {
    private String title;
    private OffsetDateTime endDate;
    private Integer totalTickets;

    public CreateDrawRequest() {
    }

    public CreateDrawRequest(String title, OffsetDateTime endDate, Integer totalTickets) {
        this.title = title;
        this.endDate = endDate;
        this.totalTickets = totalTickets;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
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
