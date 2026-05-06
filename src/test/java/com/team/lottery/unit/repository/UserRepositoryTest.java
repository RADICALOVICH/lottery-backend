package com.team.lottery.unit.repository;


import com.team.lottery.users.model.UserAuthData;
import com.team.lottery.users.model.UserResponse;
import com.team.lottery.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends BaseJdbcDrawRepositoryTest {

    private UserRepository userRepository;

    @BeforeEach
    void setUpUserRepository() {
        userRepository = new UserRepository(dataSource);
    }

    @Test
    void shouldCreateUser() throws SQLException {
        /*
        * createUser: Должен успешно создавать пользователя и возвращать ID
        * */
        String login = "new_user";
        String hash = "hash123";

        long id = userRepository.createUser(login, hash);

        assertThat(id).isPositive();
        UserAuthData found = userRepository.findByLogin(login).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.login()).isEqualTo(login);
        assertThat(found.passwordHash()).isEqualTo(hash);
        assertThat(found.role()).isEqualTo("USER");
    }

    @Test
    void shouldCheckIfLoginExists() throws SQLException {
        /*
        existsByLogin: Должен возвращать true если логин занят
        * */
        String login = "existing_user";
        userRepository.createUser(login, "pass");

        assertThat(userRepository.existsByLogin(login)).isTrue();
        assertThat(userRepository.existsByLogin("non_existent")).isFalse();
    }

    @Test
    void shouldFindByLogin() throws SQLException {
        /*
        * findByLogin: Должен находить данные для аутентификации
        * */
        String login = "auth_user";
        String hash = "secure_hash";
        userRepository.createUser(login, hash);

        UserAuthData data = userRepository.findByLogin(login).orElse(null);


        assertThat(data).isNotNull();
        assertThat(data.login()).isEqualTo(login);
        assertThat(data.passwordHash()).isEqualTo(hash);
    }

    @Test
    void shouldFindById() throws SQLException {
        /*
        findById: Должен находить пользователя по ID
        */
        long id = userRepository.createUser("find_me", "pass");

        UserResponse response = userRepository.findById(id).orElse(null);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.login()).isEqualTo("find_me");
    }

    @Test
    void shouldFindAllUsers() throws SQLException {
        /*
        * findAllUsers: Должен возвращать список всех пользователей
        * */

        List<UserResponse> initialUsers = userRepository.findAllUsers();
        int initialCount = initialUsers.size();

        userRepository.createUser("user1", "p1");
        userRepository.createUser("user2", "p2");

        List<UserResponse> users = userRepository.findAllUsers();


        assertThat(users).hasSize(initialCount + 2);

        assertThat(users).extracting(UserResponse::login)
                .contains("user1", "user2");
    }

    @Test
    void shouldFindUsersByIds() throws SQLException {
        /*
        findUsersByIds: Должен находить нескольких пользователей по списку ID
        * */

        long id1 = userRepository.createUser("multi1", "p");
        long id2 = userRepository.createUser("multi2", "p");
        long id3 = userRepository.createUser("multi3", "p");

        List<UserResponse> found = userRepository.findUsersByIds(List.of(id1, id3));

        assertThat(found).hasSize(2);
        assertThat(found).extracting(UserResponse::id)
                .containsExactlyInAnyOrder(id1, id3);
    }

    @Test
    void shouldReturnEmptyListForEmptyIds() throws SQLException {
        /*
        * findUsersByIds: Должен возвращать пустой список для пустых входных данных
        * */
        List<UserResponse> result = userRepository.findUsersByIds(List.of());

        assertThat(result).isEmpty();
    }
}