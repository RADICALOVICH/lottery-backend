package com.team.lottery.common.errors;

/**
 * Единый формат JSON-ответа на ошибку.
 *
 * Пример:
 *   { "code": "VALIDATION_FAILED", "message": "login must not be blank" }
 *
 * HTTP-статус передаётся в заголовке ответа, поэтому в тело не кладём.
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ApiException e) {
        return new ErrorResponse(e.getCode(), e.getMessage());
    }
}
