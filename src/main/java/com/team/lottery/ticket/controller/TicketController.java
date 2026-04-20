package com.team.lottery.ticket.controller;

import com.team.lottery.common.errors.UnauthorizedException;
import com.team.lottery.ticket.dto.BuyTicketResponse;
import com.team.lottery.ticket.dto.TicketResponse;
import com.team.lottery.ticket.mapper.TicketMapper;
import com.team.lottery.ticket.service.TicketService;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public void registerRoutes(RoutesConfig routes) {
        routes.post("/draws/{id}/tickets", ctx -> {
            long userId = requireUserId(ctx);
            long drawId = Long.parseLong(ctx.pathParam("id"));

            var ticket = ticketService.buyTicket(drawId, userId);

            ctx.status(200).json(new BuyTicketResponse(
                    "Ticket purchased successfully",
                    TicketMapper.toResponse(ticket)
            ));
        });

        routes.get("/me/tickets", ctx -> {
            long userId = requireUserId(ctx);

            List<TicketResponse> response = ticketService.getMyTickets(userId).stream()
                    .map(TicketMapper::toResponse)
                    .collect(Collectors.toList());

            ctx.json(response);
        });

        routes.get("/me/results", ctx -> {
            long userId = requireUserId(ctx);

            List<TicketResponse> response = ticketService.getMyResults(userId).stream()
                    .map(TicketMapper::toResponse)
                    .collect(Collectors.toList());

            ctx.json(response);
        });
    }

    private long requireUserId(Context ctx) {
        Object value = ctx.attribute("userId");

        if (value instanceof Long v) {
            return v;
        }
        if (value instanceof Integer v) {
            return v.longValue();
        }
        if (value instanceof String v) {
            return Long.parseLong(v);
        }

        throw new UnauthorizedException("Authorization required");
    }
}