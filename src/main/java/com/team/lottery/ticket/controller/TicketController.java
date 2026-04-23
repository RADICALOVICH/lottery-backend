package com.team.lottery.ticket.controller;

//import com.team.lottery.common.errors.UnauthorizedException;
import com.team.lottery.ticket.dto.BuyTicketResponse;
import com.team.lottery.ticket.dto.TicketResponse;
import com.team.lottery.ticket.mapper.TicketMapper;
import com.team.lottery.ticket.service.TicketService;
import com.team.lottery.users.service.TokenService;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.util.AuthUtil;

import java.util.List;
import java.util.stream.Collectors;

public class TicketController {

    private final TicketService ticketService;
    private final TokenService tokenService;
    // по-хорошему убрать репозитории из контроллеров
    private final UserRepository userRepository;

    public TicketController(
            TicketService ticketService,
            UserRepository userRepository,
            TokenService tokenService) {
        this.ticketService = ticketService;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public void registerRoutes(RoutesConfig routes) {
        routes.post("/draws/{id}/tickets", ctx -> {
            UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);
            long userId = currentUser.getId();
            long drawId = Long.parseLong(ctx.pathParam("id"));

            var ticket = ticketService.buyTicket(drawId, userId);

            ctx.status(200).json(new BuyTicketResponse(
                    "Ticket purchased successfully",
                    TicketMapper.toResponse(ticket)
            ));
        });

        routes.get("/me/tickets", ctx -> {
            UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);
            long userId = currentUser.getId();


            List<TicketResponse> response = ticketService.getMyTickets(userId).stream()
                    .map(TicketMapper::toResponse)
                    .collect(Collectors.toList());

            ctx.json(response);
        });

        routes.get("/me/results", ctx -> {
            UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);
            long userId = currentUser.getId();


            List<TicketResponse> response = ticketService.getMyResults(userId).stream()
                    .map(TicketMapper::toResponse)
                    .collect(Collectors.toList());

            ctx.json(response);
        });
    }
}