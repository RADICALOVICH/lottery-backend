package com.team.lottery.users.controller;

import io.javalin.http.Context;
import com.team.lottery.users.model.UserResponse;
//import com.team.lottery.users.repository.OperationHistoryRepository;
import com.team.lottery.users.repository.UserRepository;
import com.team.lottery.users.service.TokenService;
import com.team.lottery.users.util.AuthUtil;

import java.util.Map;

public class UserController {

    private final UserRepository userRepository;
    private final TokenService tokenService;
   // private final OperationHistoryRepository operationHistoryRepository; //additional feature

    public UserController(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        //this.operationHistoryRepository = new OperationHistoryRepository(); //additional feature
    }

    public void me(Context ctx) {
        try {
            UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);

            if (currentUser == null) {
                return;
            }
            //additional feature
            //operationHistoryRepository.logUsersMeViewed(
            //        currentUser.getId(),
            //        currentUser.getLogin()
            //);
            ctx.status(200).json(Map.of(
                    "id", currentUser.getId(),
                    "login", currentUser.getLogin(),
                    "role", currentUser.getRole()
            ));
        } catch (Exception e) {
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "details", e.getMessage()
            ));
        }
    }

    public void findAll(Context ctx) {
        try {
            UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);

            if (currentUser == null) {
                return;
            }

            if (AuthUtil.denyIfNotAdmin(ctx, currentUser)) {
                //additional feature
                //operationHistoryRepository.logAccessDenied(
                //        currentUser.getId(),
                //        currentUser.getLogin(),
                //        "/admin/ping",
                //        "admin_required"
                //);
                return;
            }
            //additoonal feature
            //operationHistoryRepository.logUsersListViewed(
            //        currentUser.getId(),
            //        currentUser.getLogin()
            //);

            ctx.status(200).json(userRepository.findAllUsers());
        } catch (Exception e) {
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "details", e.getMessage()
            ));
        }
    }

    public void adminPing(Context ctx) {
        try {
            UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);
            if (currentUser == null) {
                return;
            }

            if (AuthUtil.denyIfNotAdmin(ctx, currentUser)) {
                //additional feature
                //operationHistoryRepository.logAccessDenied(
                //        currentUser.getId(),
                //        currentUser.getLogin(),
                //        "/admin/ping",
                //        "admin_required"
                //);
                return;
            }
//additional feature
            //operationHistoryRepository.logAdminPing(
            //        currentUser.getId(),
            //        currentUser.getLogin()
            //);


            ctx.json(Map.of(
                    "message", "Admin access granted",
                    "login", currentUser.getLogin(),
                    "role", currentUser.getRole()
            ));
        } catch (Exception e) {
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "details", e.getMessage()
            ));
        }
    }
    public void findLoggedInUsers(Context ctx) {
        try {
            UserResponse currentUser = AuthUtil.requireUser(ctx, tokenService, userRepository);

            if (currentUser == null) {
                return;
            }

            if (AuthUtil.denyIfNotAdmin(ctx, currentUser)) {
                return;
            }

            var loggedInUserIds = tokenService.getLoggedInUserIds();
            var users = userRepository.findUsersByIds(loggedInUserIds);

            ctx.status(200).json(users);
        } catch (Exception e) {
            ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "details", e.getMessage()
            ));
        }
    }
}