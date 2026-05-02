package com.team.lottery.unit;

import com.team.lottery.ticket.dto.TicketResponse;
import com.team.lottery.ticket.mapper.TicketMapper;
import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMapperTest {

    @Test
    void toResponse_ShouldMapAllFields() {
        /*
        * Должен корректно переносить все поля из Ticket в TicketResponse.
        * */

        Instant now = Instant.now();
        Ticket ticket = new Ticket(
                1L,          // id
                10L,         // drawId
                100L,        // ownerId
                42,          // ticketNumber
                TicketStatus.SOLD,
                now          // createdAt
        );


        TicketResponse response = TicketMapper.toResponse(ticket);


        assertThat(response.id()).isEqualTo(ticket.id());
        assertThat(response.drawId()).isEqualTo(ticket.drawId());
        assertThat(response.ownerId()).isEqualTo(ticket.ownerId());
        assertThat(response.ticketNumber()).isEqualTo(ticket.ticketNumber());
        assertThat(response.status()).isEqualTo(ticket.status());
        assertThat(response.createdAt()).isEqualTo(ticket.createdAt());
    }

    @Test
    void toResponse_ShouldHandleNullOwnerId() {
        /*
        * Должен корректно обрабатывать null в ownerId.
        * */

        Ticket ticket = new Ticket(1L, 10L, null, 1, TicketStatus.AVAILABLE, Instant.now());


        TicketResponse response = TicketMapper.toResponse(ticket);


        assertThat(response.ownerId()).isNull();
    }
}
