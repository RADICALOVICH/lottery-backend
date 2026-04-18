package com.team.lottery.common.errors;

/**
 * Операция корректна по форме, но противоречит состоянию системы
 * (например, login уже занят, билет уже продан).
 * HTTP 409 Conflict.
 */
public class ConflictException extends ApiException {

    private static final int STATUS = 409;
    private static final String CODE = "CONFLICT";

    public ConflictException(String message) {
        super(STATUS, CODE, message);
    }
}
