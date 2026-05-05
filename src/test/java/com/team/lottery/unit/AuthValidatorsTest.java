package com.team.lottery.unit;

import com.team.lottery.common.errors.ValidationException;
import com.team.lottery.users.validation.AuthValidators;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthValidatorsTest {

    @Nested
    class LoginTests {
        @Test
        void passesForValidLogin() {
            assertThatCode(() -> AuthValidators.login("user.name-123"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> AuthValidators.login("  valid_user  "))
                    .doesNotThrowAnyException();
        }

        @Test
        void throwsForNullOrBlank() {
            assertThatThrownBy(() -> AuthValidators.login(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("login")
                    .hasMessageContaining("blank");
            assertThatThrownBy(() -> AuthValidators.login("   "))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void throwsForTooShort() {
            assertThatThrownBy(() -> AuthValidators.login("ab"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("at least 3");
        }

        @Test
        void throwsForTooLong() {
            assertThatThrownBy(() -> AuthValidators.login("a".repeat(51)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("at most 50");
        }

        @ParameterizedTest
        @ValueSource(strings = {"user@name", "user!", "логин", "admin#1"})
        void throwsForInvalidCharacters(String invalidLogin) {
            assertThatThrownBy(() -> AuthValidators.login(invalidLogin))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("letters, digits, dot, underscore and hyphen");
        }
    }

    @Nested
    class PasswordTests {
        @Test
        void passesForValidPassword() {
            assertThatCode(() -> AuthValidators.password("securePass123"))
                    .doesNotThrowAnyException();
        }

        @Test
        void throwsForNullOrBlank() {
            assertThatThrownBy(() -> AuthValidators.password(null))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> AuthValidators.password("   "))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void throwsForTooShort() {
            assertThatThrownBy(() -> AuthValidators.password("1234567"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("at least 8");
        }

        @Test
        void throwsForTooLongInBytes() {
            assertThatThrownBy(() -> AuthValidators.password("a".repeat(73)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("72 UTF-8 bytes");

            String longRussian = "пароль".repeat(12) + "а";
            assertThatThrownBy(() -> AuthValidators.password(longRussian))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("72 UTF-8 bytes");
        }

        @Test
        void passesForCyrillicWithinByteLimit() {
            assertThatCode(() -> AuthValidators.password("пароль123"))
                    .doesNotThrowAnyException();
        }
    }
}
