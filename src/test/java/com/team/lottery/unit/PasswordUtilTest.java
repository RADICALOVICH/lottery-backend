package com.team.lottery.unit;


import com.team.lottery.users.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class PasswordUtilTest {

    // Минимальная стоимость bcrypt — тесты должны быть быстрыми;
    // в продовом конфиге bcrypt.cost = 12, но для проверки логики разницы нет.
    private static final int TEST_LOG_ROUNDS = 4;

    private final PasswordUtil passwordUtil = new PasswordUtil(TEST_LOG_ROUNDS);

    @Test
    void shouldCreateValidHash() {
        /*
        * hashPassword: Должен создавать корректный BCrypt хеш с заданным cost.
        * */
        String password = "mySecretPassword123";
        String hash = passwordUtil.hashPassword(password);

        // Проверяем, что хеш не пустой и имеет префикс BCrypt с тем же cost, что мы задали
        assertThat(hash).isNotNull().startsWith("$2a$04$");
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        /*
        * hashPassword: Должен генерировать разные хеши для одного пароля из-за соли
        * */

        String password = "constant_password";

        String hash1 = passwordUtil.hashPassword(password);
        String hash2 = passwordUtil.hashPassword(password);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void shouldReturnTrueWhenPasswordMatches() {
        /*
        * matches: Должен возвращать true, если пароль соответствует хешу.
        * */
        String password = "correct_password";
        String hash = passwordUtil.hashPassword(password);

        boolean isMatch = passwordUtil.matches(password, hash);

        assertThat(isMatch).isTrue();
    }

    @Test
    void shouldReturnFalseWhenPasswordDoesNotMatch() {
        /*
        * matches: Должен возвращать false, если пароль не соответствует хешу
        * */
        String password = "correct_password";
        String wrongPassword = "wrong_password";
        String hash = passwordUtil.hashPassword(password);

        boolean isMatch = passwordUtil.matches(wrongPassword, hash);

        assertThat(isMatch).isFalse();
    }

    @Test
    void shouldHandleCyrillicPasswords() {
        /*
        * matches: Должен корректно обрабатывать кириллицу в паролях
        * */
        String password = "мойСекретныйПароль";
        String hash = passwordUtil.hashPassword(password);

        assertThat(passwordUtil.matches(password, hash)).isTrue();
    }
}
