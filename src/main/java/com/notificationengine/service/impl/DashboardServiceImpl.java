package com.notificationengine.service.impl;

import com.notificationengine.dto.AnalyticsSummaryResponse;
import com.notificationengine.dto.DashboardResponse;
import com.notificationengine.dto.DlqStatsResponse;
import com.notificationengine.repository.UserPreferenceRepository;
import com.notificationengine.service.AnalyticsService;
import com.notificationengine.service.DashboardService;
import com.notificationengine.service.DlqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AnalyticsService analyticsService;
    private final DlqService dlqService;
    private final UserPreferenceRepository userPreferenceRepository;
    private final com.notificationengine.repository.NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats(LocalDate from, LocalDate to) {
        log.info("Fetching dashboard stats from {} to {}", from, to);

        // 1. Fetch Analytics Summary
        AnalyticsSummaryResponse analyticsSummary = analyticsService.getSummary(from, to);

        // 2. Fetch DLQ Stats
        DlqStatsResponse dlqStats = dlqService.getStats();

        // 3. Fetch User Preference Stats
        long optIns = userPreferenceRepository.countByEnabledTrue();
        long optOuts = userPreferenceRepository.countByEnabledFalse();
        long totalPrefs = optIns + optOuts;
        double globalOptOutRate = totalPrefs > 0 ? (double) optOuts / totalPrefs : 0.0;

        // Data Integrity Harmonization:
        // If analytics rollup has not run yet (e.g. fresh DB or active day),
        // pull live metrics directly from notification repository to avoid 0 count mismatch
        long totalSent = analyticsSummary.getTotalSent();
        double deliveryRate = analyticsSummary.getDeliveryRate();
        long totalFailed = analyticsSummary.getTotalFailed();

        if (totalSent == 0) {
            long liveCount = notificationRepository.count();
            if (liveCount > 0) {
                totalSent = liveCount;
                long delivered = notificationRepository.countByStatus(com.notificationengine.entity.NotificationEntity.NotificationStatus.DELIVERED);
                long failed = notificationRepository.countByStatus(com.notificationengine.entity.NotificationEntity.NotificationStatus.FAILED);
                totalFailed = failed;
                deliveryRate = totalSent > 0 ? ((double) delivered / totalSent) * 100.0 : 0.0;
            }
        }

        Map<String, Double> channelMetrics = new HashMap<>();
        if (analyticsSummary.getCostByChannel() != null) {
            channelMetrics.putAll(analyticsSummary.getCostByChannel());
        }

        return DashboardResponse.builder()
                .totalNotifications(totalSent)
                .deliveryMetrics(DashboardResponse.DeliveryMetrics.builder()
                        .deliveryRate(deliveryRate)
                        .readRate(analyticsSummary.getReadRate())
                        .clickThroughRate(analyticsSummary.getClickThroughRate())
                        .avgDeliveryTimeMs(analyticsSummary.getAvgDeliveryTimeMs() != null ? analyticsSummary.getAvgDeliveryTimeMs() : 0.0)
                        .build())
                .channelMetrics(channelMetrics)
                .failureMetrics(DashboardResponse.FailureMetrics.builder()
                        .failureRate(analyticsSummary.getFailureRate())
                        .totalFailed(totalFailed)
                        .totalCapHits(analyticsSummary.getTotalCapHits())
                        .build())
                .dlqSummary(dlqStats)
                .userPreferenceStats(DashboardResponse.UserPreferenceStats.builder()
                        .totalUsersOptedIn(optIns)
                        .totalUsersOptedOut(optOuts)
                        .globalOptOutRate(globalOptOutRate)
                        .build())
                .build();
    }
}
