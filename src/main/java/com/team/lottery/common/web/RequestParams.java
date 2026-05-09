package com.team.lottery.common.web;

import com.team.lottery.common.errors.ValidationException;
import io.javalin.http.Context;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Парсинг path / query параметров с понятным 400 при битом значении.
 *
 * Без хелпера {@code Long.valueOf("abc")} и {@code Enum.valueOf("XXX")}
 * бросают unchecked-исключения, которые ErrorHandler превращает в 500.
 * Через хелпер — это всегда 400 {@code VALIDATION_FAILED}.
 */
public final class RequestParams {

    private RequestParams() {
    }

    /**
     * Прочитать path-параметр как long. При нечисловом значении — 400.
     */
    public static long requireLong(Context ctx, String name) {
        String raw = ctx.pathParam(name);
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException(
                    name + " must be a number, got '" + raw + "'");
        }
    }

    /**
     * Прочитать query-параметр как значение enum (case-insensitive).
     * При отсутствии или невалидном значении — 400 со списком допустимых.
     */
    public static <E extends Enum<E>> E requireEnum(
            Context ctx, String name, Class<E> enumType) {
        String raw = ctx.queryParam(name);
        if (raw == null || raw.isBlank()) {
            throw new ValidationException(name + " is required");
        }
        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(enumType.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new ValidationException(
                    name + " must be one of [" + allowed + "], got '" + raw + "'");
        }
    }
}
