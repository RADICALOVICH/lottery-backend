package com.team.lottery.draws.repository;

import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Временная in-memory реализация DrawRepository.
 *
 * Используется на этапе разработки для проверки логики без реальной JDBC-реализации.
 * Данные хранятся только в памяти приложения и не сохраняются между перезапусками.
 */
public class InMemoryDrawRepository implements DrawRepository {
    private final List<Draw> storage = new ArrayList<>();
    private Long nextId = 1L;

    @Override
    public Optional<Draw> findById(Long id) {
        for (Draw draw : storage) {
            if (draw.getId().equals(id)) {
                return Optional.of(draw);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Draw> findAll() {
        return new ArrayList<>(storage);
    }

    @Override
    public Draw save(Draw draw) {
        draw.setId(nextId++);

        if (draw.getStatus() == null) {
            draw.setStatus(DrawStatus.ACTIVE); // Имитация дефолта в БД
        }

        storage.add(draw);
        return draw;
    }

    @Override
    public List<Draw> findByStatus(DrawStatus status) {
        List<Draw> result = new ArrayList<>();

        for (Draw draw : storage) {
            if (draw.getStatus()==status) {
                result.add(draw);
            }
        }
        return result;
    }

    @Override
    public void updateStatus(Long drawId, DrawStatus status) {
        for (Draw draw : storage) {
            if (draw.getId().equals(drawId)) {
                draw.setStatus(status);
                return;
            }
        }
    }
}
