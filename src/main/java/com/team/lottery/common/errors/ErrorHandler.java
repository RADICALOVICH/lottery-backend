package com.team.lottery.common.errors;

import io.javalin.config.JavalinConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registration of exception handlers for Javalin.
 *
 * ApiException (and its subclasses) -> 4xx response with unified ErrorResponse.
 * Everything else -> 500 INTERNAL_ERROR, details in logs, generic message to client.
 */
public final class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    private ErrorHandler() {
    }

    public static void register(JavalinConfig config) {
        config.routes.exception(ApiException.class, (e, ctx) -> {
            log.warn(
                    "API error: status={} code={} message={}",
                    e.getStatusCode(),
                    e.getCode(),
                    e.getMessage()
            );
            ctx.status(e.getStatusCode()).json(ErrorResponse.of(e));
        });

        config.routes.exception(Exception.class, (e, ctx) -> {
            log.error("Unhandled exception on {} {}", ctx.method(), ctx.path(), e);
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        });
    }
}