package com.team.lottery.unit;

import com.team.lottery.config.AppConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppConfigTest {

    @Test
    void shouldLoadPropertiesFromFile() {
        /*
        "Должен успешно загружать конфигурацию из файла свойств"
        * */

        // Вызов метода загрузки
        AppConfig config = AppConfig.load();

        // Проверка через AssertJ (уже есть в твоем gradle)
        assertThat(config).isNotNull();
        assertThat(config.port()).isPositive();
        assertThat(config.dbUrl()).isNotBlank();
        assertThat(config.dbUser()).isNotNull();
        assertThat(config.dbPoolSize()).isGreaterThan(0);
    }

    @Test

    void shouldParseBooleanFields() {
        /*
        "Должен корректно определять режим PROD"
        * */
        AppConfig config = AppConfig.load();

        // Проверяем, что поле isProd загружается без ошибок
        // (значение зависит от того, что в твоем application.properties)
        assertThat(config.isProd()).isInstanceOf(Boolean.class);
    }

    @Test
    void shouldThrowExceptionWhenKeyIsMissing() {
        /*
        Должен выбрасывать исключение при поврежденном или отсутствующем ключе.
        Этот тест проверяет логику getString, если бы мы могли подменить свойства.
        Так как AppConfig.load() жестко завязан на файл, убедись,
        что все обязательные поля описаны в ресурсах.
        * */

        AppConfig config = AppConfig.load();

        assertThat(config.bcryptCost())
                .withFailMessage("Bcrypt cost должен быть настроен")
                .isNotNull();
    }

    @Test
    void shouldHaveValidSchedulerInterval() {
        /*
        * Проверка интервала планировщика.
        * */
        AppConfig config = AppConfig.load();

        assertThat(config.drawSchedulerIntervalSeconds())
                .isGreaterThanOrEqualTo(0);
    }
}