package com.team.lottery.draws.controller;

import com.team.lottery.draws.dto.CreateDrawRequest;
import com.team.lottery.draws.dto.DrawResponse;
import com.team.lottery.draws.mapper.DrawMapper;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.service.DrawService;
import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.TokenService;
import com.team.lottery.users.util.AuthUtil;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

public class AdminDrawController {
    private final DrawService drawService;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AdminDrawController(
            DrawService drawService,
            UserRepository userRepository,
            TokenService tokenService
    ) {
        this.drawService = drawService;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public void registerRoutes(RoutesConfig routes) {
        routes.post("/admin/draws", ctx -> {
            CreateDrawRequest request = ctx.bodyAsClass(CreateDrawRequest.class);

            long adminId = requireAdminId(ctx);

            Draw createdDraw = drawService.createDraw(request, adminId);

            DrawResponse response = DrawMapper.toResponse(createdDraw);

            ctx.status(201);
            ctx.json(response);
        });

        routes.post("/admin/draws/{id}/run-draw", ctx -> {
            requireAdminId(ctx);

            Long drawId = Long.valueOf(ctx.pathParam("id"));
            Draw updatedDraw = drawService.runDraw(drawId);

            DrawResponse response = DrawMapper.toResponse(updatedDraw);

            ctx.json(response);
        });
    }

    private Long requireAdminId(Context ctx) throws Exception {
        UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);
        AuthUtil.requireAdmin(currentUser);
        return currentUser.getId();
    }
}
