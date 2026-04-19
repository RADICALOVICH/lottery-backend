package com.team.lottery.draws.controller;

import com.team.lottery.draws.dto.DrawResponse;
import com.team.lottery.draws.mapper.DrawMapper;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.service.DrawService;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.common.errors.NotFoundException;
import io.javalin.Javalin;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер тиражей.
 *
 * Регистрирует HTTP-эндпоинты модуля Draws и делегирует бизнес-логику
 * в {@code DrawService}.
 */

public class DrawController {
    private final DrawService drawService;

    public DrawController(DrawService drawService) {
        this.drawService = drawService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/v1/draws", ctx -> {
            String statusParam = ctx.queryParam("status");

            List<Draw> draws;
            if (statusParam == null || statusParam.isBlank()) {
                draws = drawService.getAllDraws();
            } else {
                DrawStatus status = DrawStatus.valueOf(statusParam.toUpperCase());
                draws = drawService.getDrawsByStatus(status);
            }

            List<DrawResponse> response = draws.stream()
                    .map(DrawMapper::toResponse)
                    .collect(Collectors.toList());

            ctx.json(response);
        });

        app.get("/api/v1/draws/{id}", ctx -> {
            Long drawId = Long.valueOf(ctx.pathParam("id"));

            Draw draw = drawService.getDrawById(drawId)
                    .orElseThrow(() -> new NotFoundException("Draw not found with id: " + drawId));

            ctx.json(DrawMapper.toResponse(draw));
        });
    }
}