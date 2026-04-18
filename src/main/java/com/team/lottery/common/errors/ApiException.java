package com.team.lottery.common.errors;

/**
 * Базовая ошибка бизнес-слоя.
 *
 * Все доменные исключения наследуются отсюда, чтобы ErrorHandler
 * мог превращать их в HTTP-ответ через единственный обработчик.
 *
 * statusCode — HTTP-статус ответа.
 * code       — машинно-читаемый код ошибки для клиента (например, "NOT_FOUND").
 */
public class ApiException extends RuntimeException {

    private final int statusCode;
    private final String code;

    public ApiException(int statusCode, String code, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }

    public ApiException(int statusCode, String code, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.code = code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }
}
