package com.team.lottery.unit;

import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawRepository;
import com.team.lottery.draws.scheduler.DrawScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrawSchedulerTest {

    @Mock
    private DrawRepository drawRepository;

    @InjectMocks
    private DrawScheduler drawScheduler;

    private Draw testDraw;

    @BeforeEach
    void setUp() {
        testDraw = new Draw(100L, "Scheduled Draw", null, null, null, null, null);
    }

    @Test
    void shouldCloseEndedDraws() {
        /*
         * Должен найти просроченные тиражи и перевести их в CLOSED.
         * Розыгрыш (runDraw) шедулер НЕ запускает — это делает админ через API.
         */
        when(drawRepository.findActiveEndedDraws(any(OffsetDateTime.class)))
                .thenReturn(List.of(testDraw));

        invokeProcessEndedDraws();

        verify(drawRepository).updateStatus(100L, DrawStatus.CLOSED);
    }

    @Test
    void shouldContinueProcessingIfOneDrawFails() {
        /*
         * Должен продолжать работу, если закрытие одного тиража упало с ошибкой.
         */
        Draw failedDraw = new Draw(1L, null, null, null, null, null, null);
        Draw successDraw = new Draw(2L, null, null, null, null, null, null);

        when(drawRepository.findActiveEndedDraws(any(OffsetDateTime.class)))
                .thenReturn(List.of(failedDraw, successDraw));

        // Имитируем ошибку при закрытии первого тиража
        doThrow(new RuntimeException("DB error"))
                .when(drawRepository).updateStatus(1L, DrawStatus.CLOSED);

        invokeProcessEndedDraws();

        // Убеждаемся, что попытка закрыть была для обоих, и второй прошёл
        verify(drawRepository).updateStatus(1L, DrawStatus.CLOSED);
        verify(drawRepository).updateStatus(2L, DrawStatus.CLOSED);
    }

    private void invokeProcessEndedDraws() {
        try {
            var method = DrawScheduler.class.getDeclaredMethod("processEndedDraws");
            method.setAccessible(true);
            method.invoke(drawScheduler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
