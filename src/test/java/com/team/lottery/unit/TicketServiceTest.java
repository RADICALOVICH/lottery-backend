package com.team.lottery.unit;


import com.team.lottery.common.errors.ConflictException;
import com.team.lottery.common.errors.ForbiddenException;
import com.team.lottery.common.errors.NotFoundException;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawRepository;
import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;
import com.team.lottery.ticket.repository.TicketRepository;
import com.team.lottery.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private DrawRepository drawRepository;

    @InjectMocks
    private TicketService ticketService;

    private final long drawId = 1L;
    private final long userId = 10L;
    private final long ticketId = 100L;

    @BeforeEach
    void setUp() throws SQLException {
        // Настройка мока соединения для методов с транзакциями
        lenient().when(dataSource.getConnection()).thenReturn(connection);
    }

    @Test
    void buyTicket_Success() throws SQLException {
        /*
        buyTicket: Должен успешно купить билет.
        * */

        Draw activeDraw = createDraw(DrawStatus.ACTIVE);
        Ticket availableTicket = createTicket(ticketId, TicketStatus.AVAILABLE, null);
        Ticket soldTicket = createTicket(ticketId, TicketStatus.SOLD, userId);

        when(drawRepository.findById(drawId)).thenReturn(Optional.of(activeDraw));
        when(ticketRepository.findAnyAvailableByDrawIdForUpdate(connection, drawId))
                .thenReturn(Optional.of(availableTicket));
        when(ticketRepository.buyTicket(connection, ticketId, userId)).thenReturn(true);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(soldTicket));


        Ticket result = ticketService.buyTicket(drawId, userId);


        assertThat(result.ownerId()).isEqualTo(userId);
        assertThat(result.status()).isEqualTo(TicketStatus.SOLD);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void buyTicket_Conflict_DrawNotActive() {
        /*
        buyTicket: Ошибка, если розыгрыш не ACTIVE
        * */

        Draw inactiveDraw = createDraw(DrawStatus.COMPLETED);
        when(drawRepository.findById(drawId)).thenReturn(Optional.of(inactiveDraw));


        assertThatThrownBy(() -> ticketService.buyTicket(drawId, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void buyTicket_Conflict_NoTickets() {
        /*
        buyTicket: Ошибка, если нет свободных билетов
        * */


        when(drawRepository.findById(drawId)).thenReturn(Optional.of(createDraw(DrawStatus.ACTIVE)));
        when(ticketRepository.findAnyAvailableByDrawIdForUpdate(connection, drawId))
                .thenReturn(Optional.empty());


        assertThatThrownBy(() -> ticketService.buyTicket(drawId, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No available tickets");
    }

    @Test
    void buyTicket_RollbackOnError() throws SQLException {
        /*
        buyTicket: Должен сделать rollback при ошибке.
        * */


        when(drawRepository.findById(drawId)).thenReturn(Optional.of(createDraw(DrawStatus.ACTIVE)));

        // Подсовываем любой билет
        when(ticketRepository.findAnyAvailableByDrawIdForUpdate(eq(connection), eq(drawId)))
                .thenReturn(Optional.of(createTicket(ticketId, TicketStatus.AVAILABLE, null)));

        when(ticketRepository.buyTicket(any(Connection.class), anyLong(), anyLong()))
                .thenThrow(new RuntimeException("DB Error"));


        assertThatThrownBy(() -> ticketService.buyTicket(drawId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");

        // Проверяем, что откат транзакции был вызван
        verify(connection).rollback();
    }

    @Test
    void getMyResults_FiltersCorrectly() {
        /*
        getMyResults: Должен фильтровать только WIN и LOSE
        */

        List<Ticket> allTickets = List.of(
                createTicket(1L, TicketStatus.WIN, userId),
                createTicket(2L, TicketStatus.LOSE, userId),
                createTicket(3L, TicketStatus.SOLD, userId) // Должен отфильтроваться
        );
        when(ticketRepository.findByOwnerId(userId)).thenReturn(allTickets);


        List<Ticket> results = ticketService.getMyResults(userId);


        assertThat(results).hasSize(2);
        assertThat(results).extracting(Ticket::status)
                .containsExactlyInAnyOrder(TicketStatus.WIN, TicketStatus.LOSE);
    }

    @Test
    void getMyTicket_Success() {
        /*
        * getMyTicket: Успех, если билет принадлежит пользователю.
        * */

        Ticket myTicket = createTicket(ticketId, TicketStatus.SOLD, userId);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(myTicket));


        Ticket result = ticketService.getMyTicket(ticketId, userId);


        assertThat(result).isNotNull();
        assertThat(result.ownerId()).isEqualTo(userId);
    }

    @Test
    void getMyTicket_Forbidden() {
        /*
        getMyTicket: Forbidden, если владелец другой.
        * */
        Ticket someoneElsesTicket = createTicket(ticketId, TicketStatus.SOLD, 999L);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(someoneElsesTicket));


        assertThatThrownBy(() -> ticketService.getMyTicket(ticketId, userId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void generateTickets_CallsRepository() {
        /*
        generateTickets: Должен вызывать репозиторий.
        * */
        ticketService.generateTickets(drawId, 50);


        verify(ticketRepository).createTickets(drawId, 50);
    }



    private Draw createDraw(DrawStatus status) {
        Draw draw = new Draw();
        draw.setId(drawId);
        draw.setStatus(status);
        return draw;
    }

    private Ticket createTicket(long id, TicketStatus status, Long ownerId) {
        return new Ticket(id, drawId, ownerId, 1, status, Instant.now());
    }
}