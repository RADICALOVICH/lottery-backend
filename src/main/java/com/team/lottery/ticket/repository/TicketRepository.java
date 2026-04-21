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

    List<Ticket> findSoldByDrawId(long drawId);

    Optional<Ticket> findAnyAvailableByDrawIdForUpdate(Connection connection, long drawId);

    boolean buyTicket(Connection connection, long ticketId, long userId);

    void updateStatus(long ticketId, TicketStatus status);

    void updateStatusesByDrawIdAndCurrentStatus(long drawId, TicketStatus currentStatus, TicketStatus newStatus);

    void updateStatus(Connection connection, long ticketId, TicketStatus status);

    void  updateStatusesByDrawIdAndCurrentStatus(
            Connection connection,
            long drawId,
            TicketStatus currentStatus,
            TicketStatus newStatus
    );

    Ticket save(Ticket ticket);
}