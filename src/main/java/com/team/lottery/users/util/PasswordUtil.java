package com.team.lottery.users.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt-обёртка. Конфигурируемая стоимость хеширования (logRounds)
 * прокидывается через конструктор из {@code AppConfig.bcryptCost()}.
 */
public final class PasswordUtil {

    private final int logRounds;

    public PasswordUtil(int logRounds) {
        this.logRounds = logRounds;
    }

    public String hashPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(logRounds));
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return BCrypt.checkpw(rawPassword, passwordHash);
    }
}
