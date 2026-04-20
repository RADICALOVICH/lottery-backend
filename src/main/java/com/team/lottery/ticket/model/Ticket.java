package com.team.lottery.ticket.model;

import java.time.Instant;

public record Ticket(
        long id,
        long drawId,
        Long ownerId,
        int ticketNumber,
        TicketStatus status,
        Instant createdAt
) {
}