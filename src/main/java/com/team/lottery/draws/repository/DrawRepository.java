package com.team.lottery.draws.repository;

import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;

import java.util.List;
import java.util.Optional;

public interface DrawRepository {
    // Сохранить новый тираж
    Draw save(Draw draw);

    // Получить тираж по id, если не найден - Optional.empty()
    Optional<Draw> findById(Long id);

    // Получить все тиражи
    List<Draw> findAll();

    // Получить тиражи по статусу
    List<Draw> findByStatus(DrawStatus status);

    // Обновить статус тиража
    void updateStatus(Long drawId, DrawStatus status);
}
