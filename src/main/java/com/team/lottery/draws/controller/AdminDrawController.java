package com.team.lottery.draws.controller;

import com.team.lottery.draws.dto.CreateDrawRequest;
import com.team.lottery.draws.dto.DrawResponse;
import com.team.lottery.draws.mapper.DrawMapper;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.service.DrawService;
import com.team.lottery.users.service.AuthService;
import com.team.lottery.common.web.RequestParams;
import io.javalin.config.RoutesConfig;

public class AdminDrawController {
    private final DrawService drawService;
    private final AuthService auth;

    public AdminDrawController(DrawService drawService, AuthService auth) {
        this.drawService = drawService;
        this.auth = auth;
    }

    public void registerRoutes(RoutesConfig routes) {
        routes.post("/admin/draws", ctx -> {
            long adminId = auth.requireAdmin(ctx).id();

            CreateDrawRequest request = ctx.bodyAsClass(CreateDrawRequest.class);
            Draw createdDraw = drawService.createDraw(request, adminId);
            DrawResponse response = DrawMapper.toResponse(createdDraw);

            ctx.status(201).json(response);
        });

        routes.post("/admin/draws/{id}/run-draw", ctx -> {
            auth.requireAdmin(ctx);

            long drawId = RequestParams.requireLong(ctx, "id");
            Draw updatedDraw = drawService.runDraw(drawId);

            DrawResponse response = DrawMapper.toResponse(updatedDraw);

            ctx.json(response);
        });
    }
}