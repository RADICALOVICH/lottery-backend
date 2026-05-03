package com.team.lottery.unit;



import com.team.lottery.users.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    @Test
    void shouldSetAndGetFields() {
        /*
        Должен корректно работать с полями через геттеры и сеттеры.
        * */

        LoginRequest request = new LoginRequest();
        String expectedLogin = "admin";
        String expectedPassword = "secret_password";


        request.setLogin(expectedLogin);
        request.setPassword(expectedPassword);


        assertThat(request.getLogin()).isEqualTo(expectedLogin);
        assertThat(request.getPassword()).isEqualTo(expectedPassword);
    }

    @Test
    void shouldCreateEmptyObject() {
        /*
        Должен создавать пустой объект через конструктор по умолчанию.
        * */


        LoginRequest request = new LoginRequest();


        assertThat(request.getLogin()).isNull();
        assertThat(request.getPassword()).isNull();
    }

    @Test
    void shouldUpdateFields() {
        /*
        * "Должен корректно изменять значения полей"
        * */

        LoginRequest request = new LoginRequest();
        request.setLogin("old_login");


        request.setLogin("new_login");


        assertThat(request.getLogin()).isEqualTo("new_login");
    }
}