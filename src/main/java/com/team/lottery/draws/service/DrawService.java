package com.team.lottery.draws.service;

import com.team.lottery.common.db.Tx;
import com.team.lottery.common.errors.ConflictException;
import com.team.lottery.common.errors.NotFoundException;
import com.team.lottery.draws.dto.CreateDrawRequest;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawResult;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawRepository;
import com.team.lottery.draws.repository.DrawResultRepository;
import com.team.lottery.draws.validation.DrawValidators;
import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;
import com.team.lottery.ticket.repository.TicketRepository;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Сервис тиражей.
 * <p>
 * Отвечает за основные сценарии работы с тиражами:
 * - Создание тиража (атомарно: запись в БД + предгенерация билетов в статусе AVAILABLE);
 * - Проведение розыгрыша (CLOSED → COMPLETED, выбор победителя, обновление статусов билетов);
 * - Получение всех тиражей и фильтрация по статусу;
 * - Получение тиража и результата розыгрыша по id.
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
     * Создаёт новый тираж на основе данных из запроса администратора.
     *
     * <p>В одной транзакции:
     * <ul>
     *  <li>сохраняет тираж через {@code DrawRepository}
     *      (статус ACTIVE и created_at — дефолты на уровне БД);</li>
     *  <li>генерирует {@code totalTickets} билетов в статусе AVAILABLE
     *      с номерами от 1 до N.</li>
     * </ul>
     *
     * <p>Атомарность гарантируется {@link Tx#execute}: либо создаётся тираж
     * и все его билеты, либо ничего (нет orphan-draw без билетов).
     *
     * @param request входные данные для создания тиража
     * @param adminId идентификатор администратора, который создаёт тираж
     * @return сохранённый тираж
     */


    public Draw createDraw(CreateDrawRequest request, Long adminId) {

        DrawValidators.createDrawRequest(request);

        Draw draw = new Draw(
                null,
                request.title(),
                null,
                request.endDate(),
                request.totalTickets(),
                adminId,
                null
        );

        // Атомарность: либо создаётся тираж и все его билеты, либо ничего.
        // Без Tx.execute риск — тираж создан, но билеты не успели вставиться.
        return Tx.execute(dataSource, connection -> {
            Draw savedDraw = drawRepository.save(connection, draw);

            Instant createdAt = Instant.now();

            for (int ticketNumber = 1; ticketNumber <= savedDraw.totalTickets(); ticketNumber++) {
                Ticket ticket = new Ticket(
                        0L,
                        savedDraw.id(),
                        null,
                        ticketNumber,
                        TicketStatus.AVAILABLE,
                        createdAt
                );

                ticketRepository.save(connection, ticket);
            }

            return savedDraw;
        });
    }

    public Draw runDraw(Long drawId) {
        Draw draw = drawRepository.findById(drawId)
                .orElseThrow(() -> new NotFoundException("Draw not found with id: " + drawId));

        if (draw.status() == DrawStatus.COMPLETED) {
            throw new ConflictException("Draw is already completed");
        }

        if (draw.status() != DrawStatus.CLOSED) {
            throw new ConflictException("Draw must be CLOSED to run");
        }

        
        List<Ticket> allTickets = ticketRepository.findByDrawId(drawId);
        if (allTickets.isEmpty()) {
            throw new ConflictException("Draw cannot be run without tickets");
        }

        boolean hasSoldTickets = allTickets.stream()
                .anyMatch(t -> t.status() == TicketStatus.SOLD);
        if (!hasSoldTickets) {
            throw new ConflictException("No tickets sold - cannot run draw");
        }

        int winningIndex = ThreadLocalRandom.current().nextInt(allTickets.size());
        Ticket winningTicket = allTickets.get(winningIndex);

        Tx.execute(dataSource, connection -> {
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

            drawRepository.updateStatus(
                    connection,
                    drawId,
                    DrawStatus.COMPLETED
            );

            DrawResult drawResult = new DrawResult(
                    null,
                    drawId,
                    winningTicket.id(),
                    OffsetDateTime.now()
            );

            drawResultRepository.save(connection, drawResult);
        });

        // Перечитываем тираж после транзакции — нужны актуальные значения статуса
        // (status стал COMPLETED) для возврата клиенту.
        return drawRepository.findById(drawId)
                .orElseThrow(() -> new NotFoundException("Draw not found"));
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

}
