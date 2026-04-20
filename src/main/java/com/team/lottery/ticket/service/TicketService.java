package com.team.lottery.ticket.service;

import com.team.lottery.common.db.JdbcHelper;
import com.team.lottery.common.errors.ConflictException;
import com.team.lottery.common.errors.ForbiddenException;
import com.team.lottery.common.errors.NotFoundException;
import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;
import com.team.lottery.ticket.repository.TicketRepository;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TicketService {

    private final DataSource dataSource;
    private final TicketRepository ticketRepository;

    public TicketService(DataSource dataSource, TicketRepository ticketRepository) {
        this.dataSource = dataSource;
        this.ticketRepository = ticketRepository;
    }

    public Ticket buyTicket(long drawId, long userId) {
        return JdbcHelper.withTx(dataSource, connection -> {
            Ticket ticket = ticketRepository.findAnyAvailableByDrawIdForUpdate(connection, drawId)
                    .orElseThrow(() -> new ConflictException("No available tickets for this draw"));

            boolean updated = ticketRepository.buyTicket(connection, ticket.id(), userId);
            if (!updated) {
                throw new ConflictException("Ticket was already bought");
            }

            return ticketRepository.findById(ticket.id())
                    .orElseThrow(() -> new NotFoundException("Bought ticket not found"));
        });
    }

    public List<Ticket> getMyTickets(long userId) {
        return ticketRepository.findByOwnerId(userId);
    }

    public List<Ticket> getMyResults(long userId) {
        return ticketRepository.findByOwnerId(userId).stream()
                .filter(ticket -> ticket.status() == TicketStatus.WIN || ticket.status() == TicketStatus.LOSE)
                .toList();
    }

    public Ticket getMyTicket(long ticketId, long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));

        if (ticket.ownerId() == null || !ticket.ownerId().equals(userId)) {
            throw new ForbiddenException("Ticket does not belong to current user");
        }

        return ticket;
    }

    public List<Ticket> getTicketsByDrawId(long drawId) {
        return ticketRepository.findByDrawId(drawId);
    }

    public List<Ticket> getSoldTicketsForDraw(long drawId) {
        return ticketRepository.findSoldByDrawId(drawId);
    }

    public Ticket chooseWinningTicket(long drawId) {
        List<Ticket> soldTickets = ticketRepository.findSoldByDrawId(drawId);

        if (soldTickets.isEmpty()) {
            throw new ConflictException("No sold tickets for draw");
        }

        int winnerIndex = ThreadLocalRandom.current().nextInt(soldTickets.size());
        return soldTickets.get(winnerIndex);
    }

    public void markAllSoldAsLose(long drawId) {
        ticketRepository.updateStatusesByDrawIdAndCurrentStatus(drawId, TicketStatus.SOLD, TicketStatus.LOSE);
    }

    public void markTicketAsWin(long ticketId) {
        ticketRepository.updateStatus(ticketId, TicketStatus.WIN);
    }
}