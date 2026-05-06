package com.team.lottery.users.repository;

import com.team.lottery.users.model.UserAuthData;
import com.team.lottery.users.model.UserResponse;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    boolean existsByLogin(String login);

    long createUser(String login, String passwordHash);

    Optional<UserAuthData> findByLogin(String login);

    Optional<UserResponse> findById(long id);

    List<UserResponse> findAllUsers();

    List<UserResponse> findUsersByIds(List<Long> ids);
}
