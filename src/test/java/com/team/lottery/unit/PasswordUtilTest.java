package com.team.lottery.unit;


import com.team.lottery.users.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class PasswordUtilTest {

    @Test
    void shouldCreateValidHash() {
        /*
        * hashPassword: Должен создавать корректный BCrypt хеш.
        * */
        String password = "mySecretPassword123";
        String hash = PasswordUtil.hashPassword(password);

        // Проверяем, что хеш не пустой и имеет префикс BCrypt (версия 2a и 10 раундов)
        assertThat(hash).isNotNull().startsWith("$2a$10$");
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        /*
        * hashPassword: Должен генерировать разные хеши для одного пароля из-за соли
        * */

        String password = "constant_password";

        String hash1 = PasswordUtil.hashPassword(password);
        String hash2 = PasswordUtil.hashPassword(password);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void shouldReturnTrueWhenPasswordMatches() {
        /*
        * matches: Должен возвращать true, если пароль соответствует хешу.
        * */
        String password = "correct_password";
        String hash = PasswordUtil.hashPassword(password);

        boolean isMatch = PasswordUtil.matches(password, hash);

        assertThat(isMatch).isTrue();
    }

    @Test
    void shouldReturnFalseWhenPasswordDoesNotMatch() {
        /*
        * matches: Должен возвращать false, если пароль не соответствует хешу
        * */
        String password = "correct_password";
        String wrongPassword = "wrong_password";
        String hash = PasswordUtil.hashPassword(password);

        boolean isMatch = PasswordUtil.matches(wrongPassword, hash);

        assertThat(isMatch).isFalse();
    }

    @Test
    void shouldHandleCyrillicPasswords() {
        /*
        * matches: Должен корректно обрабатывать кириллицу в паролях
        * */
        String password = "мойСекретныйПароль";
        String hash = PasswordUtil.hashPassword(password);

        assertThat(PasswordUtil.matches(password, hash)).isTrue();
    }
}