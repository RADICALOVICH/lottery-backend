package com.team.lottery.common.validation;

import com.team.lottery.common.errors.ValidationException;

/**
 * Статические хелперы для валидации входов DTO.
 *
 * Каждый метод либо возвращается без последствий (валидация прошла),
 * либо кидает ValidationException с сообщением вида "<field>: <причина>".
 *
 * Сообщения на английском — это текст, уходящий клиенту в теле 400-ответа.
 */
public final class Validators {

    private Validators() {
    }

    public static void notNull(Object value, String field) {
        if (value == null) {
            throw new ValidationException(field + " must not be null");
        }
    }

    public static void notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " must not be blank");
        }
    }

    public static void minLen(String value, int min, String field) {
        notNull(value, field);
        if (value.length() < min) {
            throw new ValidationException(field + " must be at least " + min + " characters long");
        }
    }

    public static void maxLen(String value, int max, String field) {
        notNull(value, field);
        if (value.length() > max) {
            throw new ValidationException(field + " must be at most " + max + " characters long");
        }
    }

    public static void positive(long value, String field) {
        if (value <= 0) {
            throw new ValidationException(field + " must be positive");
        }
    }
}
