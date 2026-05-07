package com.team.lottery.unit;

import com.team.lottery.common.errors.ConflictException;
import com.team.lottery.common.errors.ValidationException;
import com.team.lottery.draws.dto.CreateDrawRequest;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawRepository;
import com.team.lottery.draws.repository.DrawResultRepository;
import com.team.lottery.draws.service.DrawService;
import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;
import com.team.lottery.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrawServiceTest {

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private DrawRepository drawRepository;
    @Mock private DrawResultRepository drawResultRepository;
    @Mock private TicketRepository ticketRepository;

    @InjectMocks
    private DrawService drawService;

    @Test
    void createDraw_Success() throws Exception {
        /*
        Должен сохранять тираж и создавать указанное количество билетов.
        * */
        Long adminId = 1L;
        CreateDrawRequest request = new CreateDrawRequest(
                "New Year Draw",
                OffsetDateTime.now().plusDays(1),
                3
        );

        Draw savedDraw = new Draw(10L, null, null, null, 3, null, null);

        when(dataSource.getConnection()).thenReturn(connection);
        when(drawRepository.save(any(Connection.class), any(Draw.class))).thenReturn(savedDraw);


        Draw result = drawService.createDraw(request, adminId);


        assertThat(result.id()).isEqualTo(10L);
        verify(drawRepository).save(any(Connection.class), any(Draw.class));
        verify(ticketRepository, times(3)).save(any(Connection.class), any(Ticket.class));
    }

    @Test
    void createDraw_PastDate_ThrowsException() {
        /*
        Должен бросать ValidationException, если дата окончания в прошлом.
        * */
        CreateDrawRequest request = new CreateDrawRequest(
                "Test draw",
                OffsetDateTime.now().minusDays(1),
                3
        );

        assertThatThrownBy(() -> drawService.createDraw(request, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("endDate must not be in the past");
    }

    @Test
    void runDraw_Success() throws Exception {
        /*
        Должен успешно проводить розыгрыш.
        */

        Long drawId = 10L;
        Draw draw = new Draw(drawId, null, DrawStatus.CLOSED, null, null, null, null);

        Ticket soldTicket = new Ticket(1L, drawId, 1L, 1, TicketStatus.SOLD, null);

        when(drawRepository.findById(drawId)).thenReturn(Optional.of(draw));
        when(ticketRepository.findByDrawId(drawId)).thenReturn(List.of(soldTicket));
        when(dataSource.getConnection()).thenReturn(connection);


        drawService.runDraw(drawId);


        verify(connection).commit();
        verify(ticketRepository).updateStatus(eq(connection), anyLong(), eq(TicketStatus.WIN));
        verify(drawRepository).updateStatus(connection, drawId, DrawStatus.COMPLETED);
        verify(drawResultRepository).save(eq(connection), any());
    }

    @Test
    void runDraw_WrongStatus_ThrowsException() {
        /*
        Должен бросать ConflictException, если статус не CLOSED.
        * */
        Long drawId = 10L;
        Draw draw = new Draw(drawId, null, DrawStatus.ACTIVE, null, null, null, null);

        when(drawRepository.findById(drawId)).thenReturn(Optional.of(draw));

        assertThatThrownBy(() -> drawService.runDraw(drawId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("must be CLOSED to run");
    }

    @Test
    void runDraw_NoSoldTickets_ThrowsException() {
        /*
        Должен бросать ConflictException, если нет проданных билетов.
        * */
        Long drawId = 10L;
        Draw draw = new Draw(drawId, null, DrawStatus.CLOSED, null, null, null, null);

        when(drawRepository.findById(drawId)).thenReturn(Optional.of(draw));
        when(ticketRepository.findByDrawId(drawId)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> drawService.runDraw(drawId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No tickets sold");
    }

    @Test
    void runDraw_RollbackOnError() throws Exception {
        /*
        Должен откатывать транзакцию при ошибке.
        * */

        Long drawId = 10L;
        Draw draw = new Draw(drawId, null, DrawStatus.CLOSED, null, null, null, null);
        Ticket ticket = new Ticket(1L, drawId, 1L, 1, TicketStatus.SOLD, null);

        when(drawRepository.findById(drawId)).thenReturn(Optional.of(draw));
        when(ticketRepository.findByDrawId(drawId)).thenReturn(List.of(ticket));
        when(dataSource.getConnection()).thenReturn(connection);

        // Имитируем ошибку при сохранении результата
        doThrow(new RuntimeException("DB Error")).when(drawResultRepository).save(any(), any());


        assertThatThrownBy(() -> drawService.runDraw(drawId))
                .isInstanceOf(RuntimeException.class);

        verify(connection).rollback();
    }
}
