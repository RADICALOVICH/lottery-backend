package com.team.lottery.common.errors;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Регистрация обработчиков исключений в Javalin.
 *
 * ApiException (и все его наследники) → 4xx-ответ с единым ErrorResponse.
 * Всё остальное → 500 INTERNAL_ERROR, детали в лог, клиенту — общее сообщение.
 */
public final class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    private ErrorHandler() {
    }

    public static void register(Javalin app) {
        app.exception(ApiException.class, (e, ctx) -> {
            log.warn("API error: status={} code={} message={}", e.getStatusCode(), e.getCode(), e.getMessage());
            ctx.status(e.getStatusCode()).json(ErrorResponse.of(e));
        });

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Unhandled exception on {} {}", ctx.method(), ctx.path(), e);
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        });
    }
}
