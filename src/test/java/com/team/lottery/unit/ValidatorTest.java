package com.team.lottery.unit;
import com.team.lottery.common.errors.ValidationException;
import com.team.lottery.common.validation.Validators;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValidatorTest {
    @Nested
    @DisplayName("notNull")
    class NotNull {
        @Test
        void shouldPassWhenValueIsPresent() {
            // Сценарий: Вызов notNull с не-null объектом
            // Ожидаемый результат: Исключение не выбрасывается.
            assertThatCode(() -> Validators.notNull("some value", "field"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowExceptionWhenValueIsNull() {
            // Сценарий: Вызов notNull с null вместо объекта
            // Ожидаемый результат: ValidationException с текстом "<field> must not be null"
            assertThatThrownBy(() -> Validators.notNull(null, "username"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("username must not be null");
        }
    }

    @Nested
    @DisplayName("notBlank")
    class NotBlank {
        @Test
        void shouldPassWhenValueHasContent() {
            // Сценарий: Передача строки с текстом
            // Ожидаемый результат: Валидация пройдена.
            assertThatCode(() -> Validators.notBlank("valid", "field"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowExceptionWhenNullOrEmptyOrSpaces() {
            // Сценарий: Передача null, "" или "   "
            // Ожидаемый результат: ValidationException с текстом о пустом поле.
            String expectedMessage = "field must not be blank";

            assertThatThrownBy(() -> Validators.notBlank(null, "field"))
                    .hasMessage(expectedMessage);

            assertThatThrownBy(() -> Validators.notBlank("", "field"))
                    .hasMessage(expectedMessage);

            assertThatThrownBy(() -> Validators.notBlank("   ", "field"))
                    .hasMessage(expectedMessage);
        }
    }

    @Nested
    @DisplayName("minLen")
    class MinLen {
        @Test
        void shouldPassWhenLengthIsEqualOrGreater() {
            // Сценарий: Передача строки длиной 3 при минимуме 3
            // Ожидаемый результат: Ошибок нет.
            assertThatCode(() -> Validators.minLen("123", 3, "field"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowWhenTooShort() {
            // Сценарий: Передача строки длиной 2 при минимуме 3
            // Ожидаемый результат: ValidationException с указанием минимальной длины.
            assertThatThrownBy(() -> Validators.minLen("12", 3, "password"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("password must be at least 3 characters long");
        }
    }

    @Nested
    @DisplayName("maxLen")
    class MaxLen {
        @Test
        void shouldPassWhenLengthIsEqualOrLess() {
            // Сценарий: Длина строки совпадает с максимально допустимой.
            // Ожидаемый результат: Валидация успешна.
            assertThatCode(() -> Validators.maxLen("abc", 3, "field"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowWhenTooLong() {
            // Сценарий: Строка длиннее, чем разрешено параметром max.
            // Результат: ValidationException с сообщением об ограничении.
            assertThatThrownBy(() -> Validators.maxLen("abcd", 3, "code"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("code must be at most 3 characters long");
        }
    }

    @Nested
    @DisplayName("positive")
    class Positive {
        @Test
        void shouldPassWhenValueIsGreaterThanZero() {
            // Сценарий: Передача положительного числа (1)
            // Ожидаемый результат: Ошибок нет.
            assertThatCode(() -> Validators.positive(1, "id"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowWhenZeroOrNegative() {
            // Сценарий: Проверка значений 0 и -5
            // Ожидаемый результат: ValidationException с текстом "must be positive".
            String expectedMessage = "amount must be positive";

            assertThatThrownBy(() -> Validators.positive(0, "amount"))
                    .hasMessage(expectedMessage);

            assertThatThrownBy(() -> Validators.positive(-5, "amount"))
                    .hasMessage(expectedMessage);
        }
    }
}
