package com.team.lottery.draws.scheduler;

import com.team.lottery.draws.model.Draw;
import com.team.lottery.draws.model.DrawStatus;
import com.team.lottery.draws.repository.DrawRepository;
import com.team.lottery.draws.service.DrawService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DrawScheduler {

    private static final Logger log = LoggerFactory.getLogger(DrawScheduler.class);

    private final DrawRepository drawRepository;
    private final DrawService drawService;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public DrawScheduler(
            DrawRepository drawRepository,
            DrawService drawService
    ) {
        this.drawRepository = drawRepository;
        this.drawService = drawService;
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
                    drawRepository.updateStatus(draw.id(), DrawStatus.CLOSED);
                    drawService.runDraw(draw.id());
                    log.info("Draw {} was processed by scheduler", draw.id());
                } catch (Exception e) {
                    log.error("Failed to process draw {} in scheduler", draw.id(), e);
                }
            }
        } catch (Exception e) {
            log.error("Scheduler iteration failed", e);
        }
    }
}