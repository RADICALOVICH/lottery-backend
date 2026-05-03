package com.team.lottery.unit;

import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawRepository;
import com.team.lottery.draws.scheduler.DrawScheduler;
import com.team.lottery.draws.service.DrawService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @Mock
    private DrawService drawService;

    @InjectMocks
    private DrawScheduler drawScheduler;

    private Draw testDraw;

    @BeforeEach
    void setUp() {
        testDraw = new Draw();
        testDraw.setId(100L);
        testDraw.setTitle("Scheduled Draw");
    }

    @Test
    void shouldProcessEndedDraws() {
        /*
        * Должен найти просроченные тиражи, закрыть их и запустить розыгрыш.
        * */


        // Репозиторий возвращает один тираж
        when(drawRepository.findActiveEndedDraws(any(OffsetDateTime.class)))
                .thenReturn(List.of(testDraw));

        // Вызываем метод напрямую (в обход планировщика), так как нам важна логика обработки
        // Мы используем рефлексию или делаем метод package-private для тестов
        // В данном случае лучше всего тестировать через вызов private метода,
        // если бы он был защищен, но мы протестируем логику внутри за счет вызова метода напрямую.
        invokeProcessEndedDraws();

        // Assert: проверяем цепочку вызовов
        verify(drawRepository).updateStatus(100L, DrawStatus.CLOSED);
        verify(drawService).runDraw(100L);
    }

    @Test
    void shouldContinueProcessingIfOneDrawFails() {
        /*
        Должен продолжать работу, если один из тиражей вызвал исключение.
        */

        // Два тиража, первый вызовет ошибку
        Draw failedDraw = new Draw();
        failedDraw.setId(1L);
        Draw successDraw = new Draw();
        successDraw.setId(2L);

        when(drawRepository.findActiveEndedDraws(any(OffsetDateTime.class)))
                .thenReturn(List.of(failedDraw, successDraw));

        // Имитируем ошибку на первом тираже
        doThrow(new RuntimeException("Service Error")).when(drawService).runDraw(1L);


        invokeProcessEndedDraws();

        // Убеждаемся, что второй тираж все равно был обработан
        verify(drawService).runDraw(1L);
        verify(drawService).runDraw(2L);
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
