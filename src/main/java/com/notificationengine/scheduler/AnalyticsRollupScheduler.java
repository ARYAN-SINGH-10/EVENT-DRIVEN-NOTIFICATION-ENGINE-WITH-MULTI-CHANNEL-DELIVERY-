package com.notificationengine.scheduler;

import com.notificationengine.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled nightly rollup that recomputes all derived analytics metrics
 * (rates, cost estimates, etc.) for the previous day from the raw counters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsRollupScheduler {

    private final AnalyticsService analyticsService;

    /**
     * Performs a full daily rollup of analytics data at 01:00 AM.
     * Recalculates delivery rate, read rate, CTR, failure rate, opt-out rate,
     * estimated cost, and all other derived metrics for yesterday.
     */
    @Scheduled(cron = "${app.scheduler.analytics-rollup-cron:0 0 1 * * ?}")
    public void dailyRollup() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[ANALYTICS-ROLLUP] Starting nightly rollup for date={}", yesterday);
        try {
            analyticsService.rollupForDate(yesterday);
            log.info("[ANALYTICS-ROLLUP] Completed nightly rollup for date={}", yesterday);
        } catch (Exception e) {
            log.error("[ANALYTICS-ROLLUP] Error during nightly rollup for date={}: {}", yesterday, e.getMessage(), e);
        }
    }
}
