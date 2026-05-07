package com.team.lottery.draws.repository;

import com.team.lottery.draws.model.DrawResult;

import java.sql.Connection;
import java.util.Optional;

public interface DrawResultRepository {
    // Сохранить результат розыгрыша в рамках транзакции (Connection приходит из Tx.execute)
    DrawResult save(Connection connection, DrawResult drawResult);

    Optional<DrawResult> findByDrawId(Long drawId);
}
