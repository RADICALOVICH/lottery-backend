package com.team.lottery.ticket.dto;

import com.team.lottery.ticket.model.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        long id,
        long drawId,
        Long ownerId,
        int ticketNumber,
        TicketStatus status,
        Instant createdAt
) {
}