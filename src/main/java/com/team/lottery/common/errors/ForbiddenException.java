package com.team.lottery.common.errors;

/**
 * Пользователь аутентифицирован, но роли недостаточно для операции
 * (например, обычный USER дёргает админскую ручку).
 * HTTP 403 Forbidden.
 */
public class ForbiddenException extends ApiException {

    private static final int STATUS = 403;
    private static final String CODE = "FORBIDDEN";

    public ForbiddenException(String message) {
        super(STATUS, CODE, message);
    }
}
