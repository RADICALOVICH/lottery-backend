package com.team.lottery.ticket.repository;

import com.team.lottery.ticket.model.Ticket;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Optional<Ticket> findAnyAvailableByDrawIdForUpdate(Connection connection, long drawId);

    boolean buyTicket(Connection connection, long ticketId, long userId);

    Optional<Ticket> findById(long ticketId);

    List<Ticket> findByOwnerId(long userId);

    void createTickets(long drawId, int totalTickets);
}