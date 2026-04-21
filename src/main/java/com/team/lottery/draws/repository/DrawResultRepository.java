package com.team.lottery.draws.repository;

import com.team.lottery.draws.model.DrawResult;

import java.sql.Connection;
import java.util.Optional;

public interface DrawResultRepository {
    DrawResult save(DrawResult drawResult);

    DrawResult saveInTransaction(Connection connection, DrawResult drawResult);

    Optional<DrawResult> findByDrawId(Long drawId);
}
