package com.team.lottery.ticket.dto;

public record BuyTicketResponse(
        String message,
        TicketResponse ticket
) {
}