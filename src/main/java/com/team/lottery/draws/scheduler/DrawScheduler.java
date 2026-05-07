package com.team.lottery.draws.scheduler;

import com.team.lottery.common.db.Tx;
import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Шедулер закрывает продажу билетов в тиражах, у которых наступила end_date:
 * переводит ACTIVE → CLOSED. Розыгрыш (CLOSED → COMPLETED) шедулер НЕ
 * проводит — это явное действие администратора через POST /admin/draws/{id}/run-draw.
 */
public class DrawScheduler {

    private static final Logger log = LoggerFactory.getLogger(DrawScheduler.class);

    private final DataSource dataSource;
    private final DrawRepository drawRepository;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public DrawScheduler(DataSource dataSource, DrawRepository drawRepository) {
        this.dataSource = dataSource;
        this.drawRepository = drawRepository;
    }

    public void start() {
        executor.scheduleWithFixedDelay(this::processEndedDraws, 10, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void processEndedDraws() {
        try {
            List<Draw> readyDraws = drawRepository.findActiveEndedDraws(OffsetDateTime.now());

            for (Draw draw : readyDraws) {
                try {
                    Tx.execute(dataSource, c -> {
                        drawRepository.updateStatus(c, draw.id(), DrawStatus.CLOSED);
                    });
                    log.info("Draw {} auto-closed by scheduler (sales ended)", draw.id());
                } catch (Exception e) {
                    log.error("Failed to close draw {} in scheduler", draw.id(), e);
                }
            }
        } catch (Exception e) {
            log.error("Scheduler iteration failed", e);
        }
    }
}
