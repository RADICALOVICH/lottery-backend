package com.team.lottery.ticket.repository;

import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Optional<Ticket> findById(long id);

    List<Ticket> findByOwnerId(long ownerId);

    List<Ticket> findByDrawId(long drawId);

    Optional<Ticket> findAnyAvailableByDrawIdForUpdate(Connection connection, long drawId);

    boolean buyTicket(Connection connection, long ticketId, long userId);

    // Обновить статус билета в рамках транзакции (Connection приходит из Tx.execute)
    void updateStatus(Connection connection, long ticketId, TicketStatus status);

    // Обновить статусы всех билетов тиража с заданным currentStatus → newStatus,
    // в рамках транзакции (Connection приходит из Tx.execute)
    void updateStatusesByDrawIdAndCurrentStatus(
            Connection connection,
            long drawId,
            TicketStatus currentStatus,
            TicketStatus newStatus
    );

    // Сохранить билет в рамках транзакции (Connection приходит из Tx.execute)
    Ticket save(Connection connection, Ticket ticket);

    // Batch insert N билетов в одном вызове (используется в тестах как fixture helper)
    void createTickets(long drawId, int totalTickets);
}
