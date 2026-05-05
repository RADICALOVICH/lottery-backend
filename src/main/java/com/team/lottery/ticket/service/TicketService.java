package com.team.lottery.ticket.service;

import com.team.lottery.common.errors.ConflictException;
import com.team.lottery.common.errors.NotFoundException;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawRepository;
import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;
import com.team.lottery.ticket.repository.TicketRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class TicketService {

    private final DataSource dataSource;
    private final TicketRepository ticketRepository;
    private final DrawRepository drawRepository;

    public TicketService(DataSource dataSource, TicketRepository ticketRepository, DrawRepository drawRepository) {
        this.dataSource = dataSource;
        this.ticketRepository = ticketRepository;
        this.drawRepository = drawRepository;
    }

    public Ticket buyTicket(long drawId, long userId) {
        Draw draw = drawRepository.findById(drawId)
                .orElseThrow(() -> new NotFoundException("Draw not found"));

        if (draw.getStatus() != DrawStatus.ACTIVE) {
            throw new ConflictException("Tickets can be bought only for ACTIVE draws");
        }

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Ticket ticket = ticketRepository.findAnyAvailableByDrawIdForUpdate(connection, drawId)
                        .orElseThrow(() -> new ConflictException("No available tickets for this draw"));

                boolean updated = ticketRepository.buyTicket(connection, ticket.id(), userId);

                if (!updated) {
                    throw new ConflictException("Ticket was already bought");
                }

                connection.commit();

                return ticketRepository.findById(ticket.id())
                        .orElseThrow(() -> new NotFoundException("Bought ticket not found"));
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
            throw new RuntimeException("Failed to buy ticket in transaction", e);
        }
    }

    public List<Ticket> getMyTickets(long userId) {
        return ticketRepository.findByOwnerId(userId);
    }

    public List<Ticket> getMyResults(long userId) {
        List<Ticket> results = ticketRepository.findByOwnerId(userId).stream()
                .filter(ticket -> ticket.status() == TicketStatus.WIN || ticket.status() == TicketStatus.LOSE)
                .toList();
        return results;
    }

}