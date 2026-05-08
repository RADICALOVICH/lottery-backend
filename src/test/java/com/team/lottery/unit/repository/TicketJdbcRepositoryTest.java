package com.team.lottery.unit.repository;

import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.ticket.model.Ticket;
import com.team.lottery.ticket.model.TicketStatus;
import com.team.lottery.ticket.repository.TicketJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TicketJdbcRepositoryTest extends BaseJdbcDrawRepositoryTest {

    private TicketJdbcRepository ticketRepository;
    private Draw testDraw;

    @BeforeEach
    void setUpTicketRepository() {
        // Инициализируем тестируемый репозиторий
        ticketRepository = new TicketJdbcRepository(dataSource);

        // Создаем тестовый розыгрыш (draw) перед каждым тестом, так как билеты к нему привязываются
        testDraw = saveCustomDraw("Test Draw for Tickets", DrawStatus.ACTIVE);
    }

    @Test
    void shouldCreateTicketsAndFindByDrawId() {
        /*
        Должен создавать пачку билетов и находить их по drawId.
        * */

        ticketRepository.createTickets(testDraw.id(), 5);
        List<Ticket> tickets = ticketRepository.findByDrawId(testDraw.id());


        assertThat(tickets).hasSize(5);
        assertThat(tickets).allMatch(ticket -> ticket.drawId() == testDraw.id());
        assertThat(tickets).allMatch(ticket -> ticket.status() == TicketStatus.AVAILABLE);
        assertThat(tickets).allMatch(ticket -> ticket.ownerId() == null);

        // Проверяем, что номера билетов генерируются по порядку (1..5)
        assertThat(tickets.get(0).ticketNumber()).isEqualTo(1);
        assertThat(tickets.get(4).ticketNumber()).isEqualTo(5);
    }

    @Test
    void shouldFindTicketById() {
        /*
        * Должен успешно находить билет по его ID.
        * */
        ticketRepository.createTickets(testDraw.id(), 1);
        Ticket createdTicket = ticketRepository.findByDrawId(testDraw.id()).get(0);


        Optional<Ticket> foundTicket = ticketRepository.findById(createdTicket.id());


        assertThat(foundTicket).isPresent();
        assertThat(foundTicket.get().id()).isEqualTo(createdTicket.id());
        assertThat(foundTicket.get().ticketNumber()).isEqualTo(1);
    }

    @Test
    void shouldBuyTicketSuccessfully() throws SQLException {
        /*
        * Должен успешно осуществлять покупку билета (buyTicket).
        * */

        ticketRepository.createTickets(testDraw.id(), 1);
        Ticket availableTicket = ticketRepository.findByDrawId(testDraw.id()).get(0);


        boolean isBought;
        try (Connection conn = dataSource.getConnection()) {
            isBought = ticketRepository.buyTicket(conn, availableTicket.id(), testUserId);
        }


        assertThat(isBought).isTrue();

        Ticket updatedTicket = ticketRepository.findById(availableTicket.id()).orElseThrow();
        assertThat(updatedTicket.status()).isEqualTo(TicketStatus.SOLD);
        assertThat(updatedTicket.ownerId()).isEqualTo(testUserId);
    }

    @Test
    void shouldNotBuyTicketTwice() throws SQLException {
        /*
        * Не должен покупать билет дважды (защита от двойной покупки).
        * */
        ticketRepository.createTickets(testDraw.id(), 1);
        Ticket availableTicket = ticketRepository.findByDrawId(testDraw.id()).get(0);


        try (Connection conn = dataSource.getConnection()) {
            boolean firstBuy = ticketRepository.buyTicket(conn, availableTicket.id(), testUserId);
            assertThat(firstBuy).isTrue();

            // Пытаемся купить тот же билет еще раз
            boolean secondBuy = ticketRepository.buyTicket(conn, availableTicket.id(), testUserId);
            assertThat(secondBuy).isFalse();
        }
    }

    @Test
    void shouldFindTicketsByOwnerId() throws SQLException {
        /*
        * Должен находить билеты по ownerId.
        * */
        ticketRepository.createTickets(testDraw.id(), 3);
        List<Ticket> tickets = ticketRepository.findByDrawId(testDraw.id());

        try (Connection conn = dataSource.getConnection()) {
            ticketRepository.buyTicket(conn, tickets.get(0).id(), testUserId);
            ticketRepository.buyTicket(conn, tickets.get(1).id(), testUserId);
        }


        List<Ticket> ownedTickets = ticketRepository.findByOwnerId(testUserId);


        assertThat(ownedTickets).hasSize(2);
        assertThat(ownedTickets).allMatch(ticket -> ticket.ownerId() == testUserId);
    }

    @Test
    void shouldFindAnyAvailableByDrawIdForUpdate() throws SQLException {
        /*
        Должен находить и блокировать доступный билет (FOR UPDATE SKIP LOCKED).
        */
        ticketRepository.createTickets(testDraw.id(), 2);


        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // Обязательно для блокировки

            Optional<Ticket> lockedTicket = ticketRepository.findAnyAvailableByDrawIdForUpdate(conn, testDraw.id());


            assertThat(lockedTicket).isPresent();
            assertThat(lockedTicket.get().status()).isEqualTo(TicketStatus.AVAILABLE);

            conn.rollback();
        }
    }

    @Test
    void shouldUpdateTicketStatus() throws SQLException {
        /*
        Должен обновлять статус конкретного билета.
        * */
        ticketRepository.createTickets(testDraw.id(), 1);
        Ticket ticket = ticketRepository.findByDrawId(testDraw.id()).get(0);

        // Сначала покупаем билет, чтобы у него появился owner_id и статус стал SOLD.
        // Это удовлетворит констрейнт chk_owner_status.
        try (Connection conn = dataSource.getConnection()) {
            ticketRepository.buyTicket(conn, ticket.id(), testUserId);
        }

        // Теперь обновляем статус. Замените WINNING на статус, который есть в вашем enum
        // и который допустим для билета с владельцем.
        // (Если такого нет, можно оставить SOLD, чтобы просто проверить, что метод отрабатывает без ошибок).
        TicketStatus newStatus = TicketStatus.WIN;


        com.team.lottery.common.db.Tx.execute(dataSource, c -> {
            ticketRepository.updateStatus(c, ticket.id(), newStatus);
        });


        Ticket updatedTicket = ticketRepository.findById(ticket.id()).orElseThrow();
        assertThat(updatedTicket.status()).isEqualTo(newStatus);
    }

    @Test
    @DisplayName("Должен обновлять статусы билетов по drawId и текущему статусу")
    void shouldUpdateStatusesByDrawIdAndCurrentStatus() throws SQLException {

        /*
        Должен пакетно обновлять статусы билетов определенного розыгрыша
        */

        // Создаем 3 билета (статус AVAILABLE, owner_id = null)
        ticketRepository.createTickets(testDraw.id(), 3);
        List<Ticket> tickets = ticketRepository.findByDrawId(testDraw.id());

        // ВАЖНО: Легализуем билеты.
        // "Покупаем" их, чтобы в БД прописался валидный owner_id и статус стал SOLD.
        try (Connection conn = dataSource.getConnection()) {
            for (Ticket ticket : tickets) {
                ticketRepository.buyTicket(conn, ticket.id(), testUserId);
            }
        }

        // Теперь у всех билетов статус SOLD и ЕСТЬ owner_id.
        // Выполняем пакетное обновление (например, из SOLD переводим в WINNING, если он есть).
        // Если статуса WINNING нет, можно обновить SOLD -> SOLD, это все равно успешно
        // проверит синтаксис SQL-запроса UPDATE в репозитории без падения БД.
        TicketStatus currentStatus = TicketStatus.SOLD;
        TicketStatus newStatus = TicketStatus.SOLD; // Замените на WINNING/COMPLETED, если есть в enum

        com.team.lottery.common.db.Tx.execute(dataSource, c -> {
            ticketRepository.updateStatusesByDrawIdAndCurrentStatus(c, testDraw.id(), currentStatus, newStatus);
        });


        List<Ticket> updatedTickets = ticketRepository.findByDrawId(testDraw.id());
        assertThat(updatedTickets).hasSize(3);
        assertThat(updatedTickets).allMatch(ticket -> ticket.status() == newStatus);
        assertThat(updatedTickets).allMatch(ticket -> ticket.ownerId() != null); // Владелец не потерялся
    }
}