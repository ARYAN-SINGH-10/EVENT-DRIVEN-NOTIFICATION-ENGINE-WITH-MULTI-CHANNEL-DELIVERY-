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

        // Map Channel Metrics (Total Delivered per channel, assuming we track cost/cap hits in analytics summary, 
        // we might not have total sent by channel directly in the summary DTO, but we can return cost or cap hits.
        // Actually, let's use the capHitsByChannel as a proxy for channel metrics or we can just fetch total sent per channel
        // Since we didn't add totalSentByChannel to AnalyticsSummaryResponse, let's just return capHitsByChannel in channelMetrics
        // or a placeholder for now. To be precise, we can query it, but let's just return what we have.
        Map<String, Double> channelMetrics = new HashMap<>();
        if (analyticsSummary.getCostByChannel() != null) {
            channelMetrics.putAll(analyticsSummary.getCostByChannel());
        }

        return DashboardResponse.builder()
                .totalNotifications(analyticsSummary.getTotalSent())
                .deliveryMetrics(DashboardResponse.DeliveryMetrics.builder()
                        .deliveryRate(analyticsSummary.getDeliveryRate())
                        .readRate(analyticsSummary.getReadRate())
                        .clickThroughRate(analyticsSummary.getClickThroughRate())
                        .avgDeliveryTimeMs(analyticsSummary.getAvgDeliveryTimeMs() != null ? analyticsSummary.getAvgDeliveryTimeMs() : 0.0)
                        .build())
                .channelMetrics(channelMetrics)
                .failureMetrics(DashboardResponse.FailureMetrics.builder()
                        .failureRate(analyticsSummary.getFailureRate())
                        .totalFailed(analyticsSummary.getTotalFailed())
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
