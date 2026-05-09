package com.team.lottery.common.errors;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.config.JavalinConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registration of exception handlers for Javalin.
 *
 * ApiException (and its subclasses) -> 4xx response with unified ErrorResponse.
 * JsonProcessingException (malformed JSON, bad date format, out-of-range numbers) -> 400.
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

        config.routes.exception(JsonProcessingException.class, (e, ctx) -> {
            log.warn("Invalid request body on {} {}: {}", ctx.method(), ctx.path(), e.getOriginalMessage());
            ctx.status(400).json(new ErrorResponse("BAD_REQUEST", "Invalid JSON request body"));
        });

        config.routes.exception(Exception.class, (e, ctx) -> {
            log.error("Unhandled exception on {} {}", ctx.method(), ctx.path(), e);
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        });
    }
}