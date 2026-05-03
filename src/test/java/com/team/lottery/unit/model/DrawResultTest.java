package com.team.lottery.unit.model;


import com.team.lottery.draws.model.DrawResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DrawResultTest {

    @Test
    void testGettersAndSetters() {
        /*
        * Проверка работы всех геттеров и сеттеров.
        * */

        DrawResult result = new DrawResult();
        Long expectedId = 1L;
        Long expectedDrawId = 100L;
        Long expectedTicketId = 555L;
        OffsetDateTime expectedTime = OffsetDateTime.now();


        result.setId(expectedId);
        result.setDrawId(expectedDrawId);
        result.setWinningTicketId(expectedTicketId);
        result.setDrawnAt(expectedTime);


        assertThat(result.getId()).isEqualTo(expectedId);
        assertThat(result.getDrawId()).isEqualTo(expectedDrawId);
        assertThat(result.getWinningTicketId()).isEqualTo(expectedTicketId);
        assertThat(result.getDrawnAt()).isEqualTo(expectedTime);
    }

    @Test

    void testNoArgsConstructor() {
        /*
        Пустой конструктор должен инициализировать поля значениями null
        * */
        DrawResult result = new DrawResult();


        assertThat(result.getId()).isNull();
        assertThat(result.getDrawId()).isNull();
        assertThat(result.getWinningTicketId()).isNull();
        assertThat(result.getDrawnAt()).isNull();
    }

    @Test

    void testFieldIndependence() {
        /*
        * Проверка независимости полей
        * */
        DrawResult result = new DrawResult();


        result.setId(1L);
        result.setDrawId(2L);


        assertThat(result.getId()).isNotEqualTo(result.getDrawId());
    }
}
