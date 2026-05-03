package com.team.lottery.unit;


import com.team.lottery.users.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    @Test

    void shouldGetAndSetFields() {

        /*
        * Должен корректно сохранять и возвращать логин и пароль через сеттеры и геттеры.
        * */

        RegisterRequest request = new RegisterRequest();
        String testLogin = "new_user_2026";
        String testPassword = "secure_password_123";


        request.setLogin(testLogin);
        request.setPassword(testPassword);


        assertThat(request.getLogin())
                .as("Проверка корректности установки логина")
                .isEqualTo(testLogin);

        assertThat(request.getPassword())
                .as("Проверка корректности установки пароля")
                .isEqualTo(testPassword);
    }

    @Test
    void shouldInitializeWithNulls() {
        /*
        Конструктор по умолчанию должен создавать объект с null значениями.
        * */

        RegisterRequest request = new RegisterRequest();

        assertThat(request.getLogin()).isNull();
        assertThat(request.getPassword()).isNull();
    }

    @Test
    void shouldAllowOverwritingFields() {
        /*
        Должен позволять перезаписывать существующие значения
        * */

        RegisterRequest request = new RegisterRequest();
        request.setLogin("initial");


        request.setLogin("updated");


        assertThat(request.getLogin()).isEqualTo("updated");
    }
}