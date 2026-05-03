package com.team.lottery.unit.model;

import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DrawTest {

    @Test

    void constructorWithAllArgsTest() {
        /*
        * Конструктор со всеми аргументами должен корректно инициализировать поля.
        * */
        Long id = 1L;
        String title = "Super Lottery";
        DrawStatus status = DrawStatus.ACTIVE;
        OffsetDateTime endDate = OffsetDateTime.now().plusDays(7);
        Integer totalTickets = 100;
        Long createdBy = 42L;
        OffsetDateTime createdAt = OffsetDateTime.now();


        Draw draw = new Draw(id, title, status, endDate, totalTickets, createdBy, createdAt);


        assertThat(draw.getId()).isEqualTo(id);
        assertThat(draw.getTitle()).isEqualTo(title);
        assertThat(draw.getStatus()).isEqualTo(status);
        assertThat(draw.getEndDate()).isEqualTo(endDate);
        assertThat(draw.getTotalTickets()).isEqualTo(totalTickets);
        assertThat(draw.getCreatedBy()).isEqualTo(createdBy);
        assertThat(draw.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void settersAndGettersTest() {
        /*
        * Сеттеры должны изменять значения полей.
        * */
        Draw draw = new Draw();
        String newTitle = "New Year Draw";
        OffsetDateTime newEndDate = OffsetDateTime.now().plusMonths(1);


        draw.setId(10L);
        draw.setTitle(newTitle);
        draw.setStatus(DrawStatus.CLOSED);
        draw.setEndDate(newEndDate);
        draw.setTotalTickets(500);
        draw.setCreatedBy(99L);
        draw.setCreatedAt(newEndDate.minusDays(1));


        assertThat(draw.getId()).isEqualTo(10L);
        assertThat(draw.getTitle()).isEqualTo(newTitle);
        assertThat(draw.getStatus()).isEqualTo(DrawStatus.CLOSED);
        assertThat(draw.getEndDate()).isEqualTo(newEndDate);
        assertThat(draw.getTotalTickets()).isEqualTo(500);
        assertThat(draw.getCreatedBy()).isEqualTo(99L);
        assertThat(draw.getCreatedAt()).isNotNull();
    }

    @Test
    void noArgsConstructorTest() {
        /*
        Пустой конструктор должен создавать объект с null полями.
        * */
        Draw draw = new Draw();

        assertThat(draw.getId()).isNull();
        assertThat(draw.getTitle()).isNull();
        assertThat(draw.getStatus()).isNull();
    }
}
