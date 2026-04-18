package com.team.lottery.common.errors;

/**
 * Запрос не аутентифицирован: токен отсутствует/невалиден,
 * либо неверные login/password при входе.
 * HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends ApiException {

    private static final int STATUS = 401;
    private static final String CODE = "UNAUTHORIZED";

    public UnauthorizedException(String message) {
        super(STATUS, CODE, message);
    }
}
