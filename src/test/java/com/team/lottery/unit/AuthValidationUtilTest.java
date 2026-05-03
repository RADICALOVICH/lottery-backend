package com.team.lottery.unit;

import com.team.lottery.users.util.AuthValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;


class AuthValidationUtilTest {

    @Nested
    class LoginValidationTests {
        @Test
        void shouldReturnNullForValidLogin() {
            /*
            * Должен вернуть null для корректного логина
            * */
            assertThat(AuthValidationUtil.validateLogin("user.name-123")).isNull();
            assertThat(AuthValidationUtil.validateLogin("  valid_user  ")).isNull(); // Проверка trim()
        }

        @Test
        void shouldReturnErrorForEmptyLogin() {
            /*
            * Должен вернуть ошибку, если логин null или пустой
            * */
            assertThat(AuthValidationUtil.validateLogin(null)).isEqualTo("Login is required");
            assertThat(AuthValidationUtil.validateLogin("   ")).isEqualTo("Login is required");
        }

        @Test
        void shouldReturnErrorForShortLogin() {
            /*
            * Должен вернуть ошибку, если логин слишком короткий
            * */
            assertThat(AuthValidationUtil.validateLogin("ab")).isEqualTo("Login must be at least 3 characters long");
        }

        @Test
        void shouldReturnErrorForLongLogin() {
            /*
            * Должен вернуть ошибку, если логин слишком длинный
            * */
            String longLogin = "a".repeat(51);
            assertThat(AuthValidationUtil.validateLogin(longLogin)).isEqualTo("Login must not be longer than 50 characters");
        }

        @ParameterizedTest
        @ValueSource(strings = {"user@name", "user!", "логин", "admin#1"})
        void shouldReturnErrorForInvalidCharacters(String invalidLogin) {
            /*
            * Должен вернуть ошибку при наличии недопустимых символов
            * */
            assertThat(AuthValidationUtil.validateLogin(invalidLogin))
                    .isEqualTo("Login may contain only letters, digits, dot, underscore and hyphen");
        }
    }

    @Nested
    class PasswordValidationTests {

        @Test
        void shouldReturnNullForValidPassword() {
            /*
            * Должен вернуть null для корректного пароля
            * */
            assertThat(AuthValidationUtil.validatePassword("securePass123")).isNull();
        }

        @Test
        void shouldReturnErrorForEmptyPassword() {
            /*
            * Должен вернуть ошибку, если пароль null или пустой
            * */
            assertThat(AuthValidationUtil.validatePassword(null)).isEqualTo("Password is required");
            assertThat(AuthValidationUtil.validatePassword("   ")).isEqualTo("Password is required");
        }

        @Test
        void shouldReturnErrorForShortPassword() {
            /*
            * Должен вернуть ошибку, если пароль короче 8 символов
            * */
            assertThat(AuthValidationUtil.validatePassword("1234567")).isEqualTo("Password must be at least 8 characters long");
        }

        @Test
        void shouldReturnErrorForTooLongPasswordInBytes() {
            /*
            * Должен вернуть ошибку, если пароль превышает 72 байта (ограничение bcrypt)
            * */

            // Английские символы = 1 байт. 73 символа = 73 байта.
            String longPass = "a".repeat(73);
            assertThat(AuthValidationUtil.validatePassword(longPass))
                    .isEqualTo("Password is too long for bcrypt (max 72 UTF-8 bytes)");

            // Кириллица = 2 байта. 37 символов * 2 = 74 байта.
            String longRussianPass = "пароль".repeat(12) + "а";
            assertThat(AuthValidationUtil.validatePassword(longRussianPass))
                    .isEqualTo("Password is too long for bcrypt (max 72 UTF-8 bytes)");
        }

        @Test
        void shouldHandleCyrillicWithinByteLimit() {
            /*
            * Должен успешно валидировать пароль на кириллице в рамках 72 байт
            * */


            // "пароль123" = 6*2 + 3 = 15 байт. Это валидно.
            assertThat(AuthValidationUtil.validatePassword("пароль123")).isNull();
        }
    }
}