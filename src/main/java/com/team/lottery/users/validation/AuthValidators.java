package com.team.lottery.users.validation;

import com.team.lottery.common.errors.ValidationException;
import com.team.lottery.common.validation.Validators;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class AuthValidators {

    private static final Pattern LOGIN_PATTERN =
            Pattern.compile("^[A-Za-z0-9._-]+$");

    private AuthValidators() {
    }

    public static void login(String login) {
        Validators.notBlank(login, "login");
        String trimmed = login.trim();
        Validators.minLen(trimmed, 3, "login");
        Validators.maxLen(trimmed, 50, "login");
        if (!LOGIN_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException(
                    "login may contain only letters, digits, dot, underscore and hyphen");
        }
    }

    public static void password(String password) {
        Validators.notBlank(password, "password");
        Validators.minLen(password, 8, "password");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException(
                    "password is too long for bcrypt (max 72 UTF-8 bytes)");
        }
    }
}
