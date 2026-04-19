package com.team.lottery.draws.service;

import com.team.lottery.common.errors.ConflictException;
import com.team.lottery.common.errors.NotFoundException;
import com.team.lottery.common.validation.Validators;
import com.team.lottery.draws.dto.CreateDrawRequest;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawResult;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawRepository;
import com.team.lottery.draws.repository.DrawResultRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис тиражей.
 *
 * Отвечает за основные сценарии работы с тиражами:
 * - Создание тиража (создание записи в БД, генерация билетов(еще нереализовано));
 * - Получение всех тиражей;
 * - Получение тиражей по статусу;
 * - Получение тиража по id;
 * - Обновление статуса тиража;
 */

public class DrawService {
    private final DrawRepository drawRepository;
    private final DrawResultRepository drawResultRepository;

    public DrawService(DrawRepository drawRepository, DrawResultRepository drawResultRepository) {
        this.drawRepository = drawRepository;
        this.drawResultRepository = drawResultRepository;
    }

    /**
     * Создает новый тираж на основе данных из запроса администратора.
     *
     * <p>На этом этапе сервис:
     * <ul>
     *  <li>собирает объект {@code Draw};</li>
     *  <li>проставляет идентификатор администратора как создателя;</li>
     *  <li>сохраняет тираж через {@code DrawRepository}.</li>
     * </ul>
     *
     * <p>Статус нового тиража и время создания задаются на уровне БД
     * через значения по умолчанию.
     *
     *  <p>Логика генерации билетов пока не реализована и будет добавлена позже
     *  как отдельный шаг сценария создания тиража.
     *
     * @param request входные данные для создания тиража
     * @param adminId идентификатор администратора, который создает тираж
     * @return сохраненный тираж
     */


    public Draw createDraw(CreateDrawRequest request, Long adminId) {
        Validators.notBlank(request.getTitle(), "title");
        Validators.notNull(request.getEndDate(), "endDate");
        Validators.notNull(request.getTotalTickets(), "totalTickets");
        Validators.positive(request.getTotalTickets(), "totalTickets");
        Draw draw = new Draw();
        draw.setTitle(request.getTitle());
        draw.setEndDate(request.getEndDate());
        draw.setTotalTickets(request.getTotalTickets());
        draw.setCreatedBy(adminId);

        Draw savedDraw = drawRepository.save(draw);

        // TODO: здесь потом нужно вызвать ticket-логику
        // ticketService.generateTickets(savedDraw.getId(), savedDraw.getTotalTickets());

        return savedDraw;
    }

    public void runDraw(Long drawId) {
        Draw draw = drawRepository.findById(drawId).
                orElseThrow(() -> new NotFoundException("Draw not found with id: " + drawId));

        if (draw.getStatus() != DrawStatus.CLOSED) {
            throw new ConflictException("Draw must be CLOSED to run");
        }

        updateDrawStatus(drawId, DrawStatus.COMPLETED);

        DrawResult drawResult = new DrawResult();
        drawResult.setDrawId(drawId);
        drawResult.setDrawnAt(OffsetDateTime.now());

        drawResultRepository.save(drawResult);
    }

    public List<Draw> getAllDraws() {
        return drawRepository.findAll();
    }

    public List<Draw> getDrawsByStatus(DrawStatus status) {
        return drawRepository.findByStatus(status);
    }

    /**
     * Возвращает тираж по идентификатору.
     *
     * @param id идентификатор тиража
     * @return найденный тираж или пустой {@code Optional}, если запись о тираже не найдена
     */

    public Optional<Draw> getDrawById(Long id) {
        return drawRepository.findById(id);
    }

    public Optional<DrawResult> getDrawResultByDrawId(Long drawId) {
        return drawResultRepository.findByDrawId(drawId);
    }

    public void updateDrawStatus(Long drawId, DrawStatus status) {
        drawRepository.updateStatus(drawId, status);
    }
}
