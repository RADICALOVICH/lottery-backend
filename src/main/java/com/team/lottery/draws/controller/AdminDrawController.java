package com.team.lottery.draws.controller;

import com.team.lottery.draws.dto.CreateDrawRequest;
import com.team.lottery.draws.dto.DrawResponse;
import com.team.lottery.draws.mapper.DrawMapper;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.service.DrawService;
import io.javalin.Javalin;


public class AdminDrawController {
    private final DrawService drawService;

    public AdminDrawController(DrawService drawService) {
        this.drawService = drawService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/admin/draws", ctx -> {
            CreateDrawRequest request = ctx.bodyAsClass(CreateDrawRequest.class);

            // Временная заглушка до появления auth/security
            long adminId = 1L;

            Draw createdDraw = drawService.createDraw(request, adminId);

            DrawResponse response = DrawMapper.toResponse(createdDraw);

            ctx.status(201);
            ctx.json(response);
        });

        app.post("/admin/draws/{id}/run-draw", ctx -> {
            Long drawId = Long.valueOf(ctx.pathParam("id"));
            drawService.runDraw(drawId);
        });
    }
}
