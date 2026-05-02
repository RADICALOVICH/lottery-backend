package com.team.lottery.unit.model;



import com.team.lottery.users.model.UserAuthData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthDataTest {

    @Test
    @DisplayName("")
    void shouldInitializeFieldsThroughConstructor() {
        /*
        * Должен корректно инициализировать все поля через конструктор.
        * */

        long expectedId = 42L;
        String expectedLogin = "lottery_winner";
        String expectedHash = "$2a$10$xyz789";
        String expectedRole = "USER";


        UserAuthData authData = new UserAuthData(
                expectedId,
                expectedLogin,
                expectedHash,
                expectedRole
        );


        assertThat(authData.getId()).isEqualTo(expectedId);
        assertThat(authData.getLogin()).isEqualTo(expectedLogin);
        assertThat(authData.getPasswordHash()).isEqualTo(expectedHash);
        assertThat(authData.getRole()).isEqualTo(expectedRole);
    }

    @Test
    void shouldHandleNullValues() {
        /*
        * Должен корректно работать с null значениями, если они переданы.
        * */

        UserAuthData authData = new UserAuthData(1L, null, null, null);


        assertThat(authData.getLogin()).isNull();
        assertThat(authData.getPasswordHash()).isNull();
        assertThat(authData.getRole()).isNull();
    }

}