package com.team.lottery.common.errors;

/**
 * Входные данные не прошли валидацию.
 * HTTP 400 Bad Request.
 */
public class ValidationException extends ApiException {

    private static final int STATUS = 400;
    private static final String CODE = "VALIDATION_FAILED";

    public ValidationException(String message) {
        super(STATUS, CODE, message);
    }
}
