package com.team.lottery.common.errors;

/**
 * Запрошенная сущность не найдена.
 * HTTP 404 Not Found.
 */
public class NotFoundException extends ApiException {

    private static final int STATUS = 404;
    private static final String CODE = "NOT_FOUND";

    public NotFoundException(String message) {
        super(STATUS, CODE, message);
    }
}
