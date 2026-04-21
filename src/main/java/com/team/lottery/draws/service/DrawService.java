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
import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;
import com.team.lottery.ticket.repository.TicketRepository;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Сервис тиражей.
 * <p>
 * Отвечает за основные сценарии работы с тиражами:
 * - Создание тиража (создание записи в БД, генерация билетов(еще нереализовано));
 * - Получение всех тиражей;
 * - Получение тиражей по статусу;
 * - Получение тиража по id;
 * - Обновление статуса тиража;
 */

public class DrawService {
    private final DataSource dataSource;
    private final DrawRepository drawRepository;
    private final DrawResultRepository drawResultRepository;
    private final TicketRepository ticketRepository;

    public DrawService(
            DataSource dataSource,
            DrawRepository drawRepository,
            DrawResultRepository drawResultRepository,
            TicketRepository ticketRepository
    ) {
        this.dataSource = dataSource;
        this.drawRepository = drawRepository;
        this.drawResultRepository = drawResultRepository;
        this.ticketRepository = ticketRepository;
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

        Instant createdAt = Instant.now();

        for (int ticketNumber = 1; ticketNumber <= savedDraw.getTotalTickets(); ticketNumber++) {
            Ticket ticket = new Ticket(
                    0L,
                    savedDraw.getId(),
                    null,
                    ticketNumber,
                    TicketStatus.AVAILABLE,
                    createdAt
            );

            ticketRepository.save(ticket);
        }

        return savedDraw;
    }

    public void runDraw(Long drawId) {
        Draw draw = drawRepository.findById(drawId)
                .orElseThrow(() -> new NotFoundException("Draw not found with id: " + drawId));

        if (draw.getStatus() == DrawStatus.COMPLETED) {
            throw new ConflictException("Draw is already completed");
        }

        if (draw.getStatus() != DrawStatus.CLOSED) {
            throw new ConflictException("Draw must be CLOSED to run");
        }

        List<Ticket> allTickets = ticketRepository.findByDrawId(drawId);
        if (allTickets.isEmpty()) {
            throw new ConflictException("Draw cannot be run without tickets");
        }

        int winningIndex = ThreadLocalRandom.current().nextInt(allTickets.size());
        Ticket winningTicket = allTickets.get(winningIndex);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try {
                ticketRepository.updateStatusesByDrawIdAndCurrentStatus(
                        connection,
                        drawId,
                        TicketStatus.SOLD,
                        TicketStatus.LOSE
                );

                ticketRepository.updateStatus(
                        connection,
                        winningTicket.id(),
                        TicketStatus.WIN
                );

                drawRepository.updateStatusInTransaction(
                        connection,
                        drawId,
                        DrawStatus.COMPLETED
                );

                DrawResult drawResult = new DrawResult();
                drawResult.setDrawId(drawId);
                drawResult.setWinningTicketId(winningTicket.id());
                drawResult.setDrawnAt(OffsetDateTime.now());

                drawResultRepository.saveInTransaction(connection, drawResult);

                connection.commit();
            } catch (Exception e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to run draw in transaction", e);
        }
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
