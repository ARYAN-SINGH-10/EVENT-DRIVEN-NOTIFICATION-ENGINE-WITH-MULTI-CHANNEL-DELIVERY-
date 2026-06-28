package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dashboard-level summary aggregating all 8 pipeline metrics
 * across all channels for a requested date range.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated analytics summary across all channels for a date range")
public class AnalyticsSummaryResponse {

    private LocalDate from;
    private LocalDate to;

    // --- Totals ---
    private long totalSent;
    private long totalDelivered;
    private long totalFailed;
    private long totalRead;
    private long totalClicked;
    private long totalOptOut;
    private long totalCapHits;

    // --- Aggregate rates ---
    @Schema(description = "Overall delivery rate = totalDelivered / totalSent")
    private Double deliveryRate;

    @Schema(description = "Overall failure rate = totalFailed / totalSent")
    private Double failureRate;

    @Schema(description = "Overall read rate = totalRead / totalDelivered")
    private Double readRate;

    @Schema(description = "Overall click-through rate = totalClicked / totalSent")
    private Double clickThroughRate;

    @Schema(description = "Overall opt-out rate = totalOptOut / totalSent")
    private Double optOutRate;

    // --- Timing ---
    @Schema(description = "Average delivery time in milliseconds across all channels")
    private Double avgDeliveryTimeMs;

    // --- Cost ---
    @Schema(description = "Total estimated cost in USD for the period")
    private Double totalCostUsd;

    @Schema(description = "Average cost per notification in USD")
    private Double avgCostPerNotification;

    /** Per-channel cost breakdown */
    private Map<String, Double> costByChannel;

    /** Per-channel frequency cap hits */
    private Map<String, Long> capHitsByChannel;

    /** Daily time series of delivery rates for trending charts */
    private List<DailyMetricPoint> deliveryRateTrend;

    /** Daily time series of failure rates */
    private List<DailyMetricPoint> failureRateTrend;
}
