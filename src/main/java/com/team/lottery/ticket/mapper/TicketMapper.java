package com.team.lottery.ticket.mapper;

import com.team.lottery.ticket.dto.TicketResponse;
import com.team.lottery.ticket.model.Ticket;

public final class TicketMapper {

    private TicketMapper() {
    }

    public static TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.id(),
                ticket.drawId(),
                ticket.ownerId(),
                ticket.ticketNumber(),
                ticket.status(),
                ticket.createdAt()
        );
    }
}