package com.team.lottery.unit;

import com.team.lottery.users.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
    }

    @Test
    void shouldGenerateNewToken() {
        /*
        * generateOrGetToken: Должен генерировать новый токен, если его нет
        * */
        long userId = 1L;
        String token = tokenService.generateOrGetToken(userId);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(tokenService.hasToken(userId)).isTrue();
    }

    @Test
    void shouldReturnExistingToken() {
        /*
        * generateOrGetToken: Должен возвращать существующий токен для того же пользователя
        * */
        long userId = 1L;
        String firstToken = tokenService.generateOrGetToken(userId);
        String secondToken = tokenService.generateOrGetToken(userId);

        assertThat(firstToken).isEqualTo(secondToken);
    }

    @Test
    void shouldGetUserIdByToken() {
        /*
        * getUserIdByToken: Должен возвращать ID пользователя по валидному токену
        * */
        long userId = 42L;
        String token = tokenService.generateOrGetToken(userId);

        Long foundUserId = tokenService.getUserIdByToken(token);

        assertThat(foundUserId).isEqualTo(userId);
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        /*
        * "getUserIdByToken: Должен возвращать null для несуществующего токена"
        * */
        Long foundUserId = tokenService.getUserIdByToken("invalid-token");

        assertThat(foundUserId).isNull();
    }

    @Test
    void shouldGetTokenByUserId() {
        /*
        getTokenByUserId: Должен возвращать токен по ID пользователя.
        */
        long userId = 10L;
        String token = tokenService.generateOrGetToken(userId);

        String foundToken = tokenService.getTokenByUserId(userId);

        assertThat(foundToken).isEqualTo(token);
    }

    @Test
    void shouldRemoveToken() {
        /*
        * removeToken: Должен удалять токен и очищать обе мапы
        * */
        long userId = 5L;
        String token = tokenService.generateOrGetToken(userId);

        tokenService.removeToken(token);

        assertThat(tokenService.getUserIdByToken(token)).isNull();
        assertThat(tokenService.hasToken(userId)).isFalse();
        assertThat(tokenService.getTokenByUserId(userId)).isNull();
    }

    @Test
    void shouldReturnLoggedInUserIds() {
        /*
        * getLoggedInUserIds: Должен возвращать список всех авторизованных пользователей.
        * */
        tokenService.generateOrGetToken(1L);
        tokenService.generateOrGetToken(2L);
        tokenService.generateOrGetToken(3L);

        List<Long> loggedInIds = tokenService.getLoggedInUserIds();

        assertThat(loggedInIds).hasSize(3)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void shouldNotThrowExceptionWhenRemovingNonExistentToken() {
        /*
        * removeToken: Удаление несуществующего токена не должно вызывать ошибок.
        * */
        tokenService.removeToken("non-existent");
        // Если тест дошел сюда без исключений, он считается пройденным
    }
}