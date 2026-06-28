package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Aggregated dashboard response combining analytics, DLQ stats, and user preference stats.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {

    private long totalNotifications;

    private DeliveryMetrics deliveryMetrics;
    private Map<String, Double> channelMetrics;
    private FailureMetrics failureMetrics;
    private DlqStatsResponse dlqSummary;
    private UserPreferenceStats userPreferenceStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryMetrics {
        private double deliveryRate;
        private double readRate;
        private double clickThroughRate;
        private double avgDeliveryTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureMetrics {
        private double failureRate;
        private long totalFailed;
        private long totalCapHits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPreferenceStats {
        private long totalUsersOptedIn;
        private long totalUsersOptedOut;
        private double globalOptOutRate;
    }
}
