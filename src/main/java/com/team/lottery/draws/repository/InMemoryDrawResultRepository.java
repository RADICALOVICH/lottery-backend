package com.team.lottery.draws.repository;

import com.team.lottery.draws.model.DrawResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryDrawResultRepository implements DrawResultRepository {
    private final List<DrawResult> storage = new ArrayList<>();
    private Long nextId = 1L;

    @Override
    public DrawResult save(DrawResult drawResult) {
        drawResult.setId(nextId++);
        storage.add(drawResult);
        return drawResult;
    }

    @Override
    public Optional<DrawResult> findByDrawId(Long drawId) {
        for (DrawResult drawResult : storage) {
            if (drawResult.getDrawId().equals(drawId)) {
                return Optional.of(drawResult);
            }
        }
        return Optional.empty();
    }
}