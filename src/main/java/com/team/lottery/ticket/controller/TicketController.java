package com.team.lottery.ticket.controller;

import com.team.lottery.common.errors.ForbiddenException;
import com.team.lottery.ticket.dto.BuyTicketResponse;
import com.team.lottery.ticket.dto.TicketResponse;
import com.team.lottery.ticket.mapper.TicketMapper;
import com.team.lottery.ticket.service.TicketService;
import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.service.AuthService;
import com.team.lottery.common.web.RequestParams;
import io.javalin.config.RoutesConfig;

import java.util.List;
import java.util.stream.Collectors;

public class TicketController {

    private final TicketService ticketService;
    private final AuthService auth;

    public TicketController(TicketService ticketService, AuthService auth) {
        this.ticketService = ticketService;
        this.auth = auth;
    }

    public void registerRoutes(RoutesConfig routes) {
        routes.post("/draws/{id}/tickets", ctx -> {
            UserResponse user = auth.requireUser(ctx);
            if ("ADMIN".equals(user.role())) {
                throw new ForbiddenException("Admins are not allowed to buy tickets");
            }
            long drawId = RequestParams.requireLong(ctx, "id");

            var ticket = ticketService.buyTicket(drawId, user.id());

            ctx.status(200).json(new BuyTicketResponse(
                    "Ticket purchased successfully",
                    TicketMapper.toResponse(ticket)
            ));
        });

        routes.get("/me/tickets", ctx -> {
            long userId = auth.requireUser(ctx).id();

            List<TicketResponse> response = ticketService.getMyTickets(userId).stream()
                    .map(TicketMapper::toResponse)
                    .collect(Collectors.toList());

            ctx.json(response);
        });

        routes.get("/me/results", ctx -> {
            long userId = auth.requireUser(ctx).id();

            List<TicketResponse> response = ticketService.getMyResults(userId).stream()
                    .map(TicketMapper::toResponse)
                    .collect(Collectors.toList());

            ctx.json(response);
        });
    }
}