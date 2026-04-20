package com.team.lottery.ticket.controller;

import com.team.lottery.common.errors.UnauthorizedException;
import com.team.lottery.ticket.dto.BuyTicketResponse;
import com.team.lottery.ticket.dto.TicketResponse;
import com.team.lottery.ticket.mapper.TicketMapper;
import com.team.lottery.ticket.service.TicketService;
import com.team.lottery.users.service.TokenService;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class TicketController {

    private final TicketService ticketService;
    private final TokenService tokenService;

    public TicketController(TicketService ticketService, TokenService tokenService) {
        this.ticketService = ticketService;
        this.tokenService = tokenService;
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
        String authHeader = ctx.header("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authorization required");
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        Long userId = tokenService.getUserIdByToken(token);

        if (userId == null) {
            throw new UnauthorizedException("Authorization required");
        }

        return userId;
    }
}