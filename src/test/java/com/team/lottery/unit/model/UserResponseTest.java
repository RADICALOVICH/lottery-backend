package com.team.lottery.unit.model;



import com.team.lottery.users.model.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserResponseTest {

    @Test
    void shouldCreateUserResponseWithCorrectFields() {
        /*
        * Должен корректно инициализировать поля id, login и role через конструктор.
        * */


        long expectedId = 55L;
        String expectedLogin = "player_one";
        String expectedRole = "USER";


        UserResponse response = new UserResponse(expectedId, expectedLogin, expectedRole);


        assertThat(response.getId()).isEqualTo(expectedId);
        assertThat(response.getLogin()).isEqualTo(expectedLogin);
        assertThat(response.getRole()).isEqualTo(expectedRole);
    }

    @Test
    void gettersShouldReturnProperValues() {
        /*
        * Должен корректно возвращать значения через геттеры.
        * */


        UserResponse response = new UserResponse(10L, "admin_user", "ADMIN");


        assertThat(response.getId()).as("ID пользователя").isEqualTo(10L);
        assertThat(response.getLogin()).as("Логин пользователя").isEqualTo("admin_user");
        assertThat(response.getRole()).as("Роль пользователя").isEqualTo("ADMIN");
    }

    @Test
    void shouldHandleNullInputs() {
        /*
        * Должен допускать null для строковых полей (login и role).
        * */


        UserResponse response = new UserResponse(1L, null, null);


        assertThat(response.getLogin()).isNull();
        assertThat(response.getRole()).isNull();
    }
}