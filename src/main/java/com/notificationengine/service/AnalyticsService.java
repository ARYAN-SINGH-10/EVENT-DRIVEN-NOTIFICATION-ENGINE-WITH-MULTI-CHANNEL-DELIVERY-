package com.notificationengine.service;

import com.notificationengine.dto.AnalyticsResponse;
import com.notificationengine.dto.AnalyticsSummaryResponse;
import com.notificationengine.entity.NotificationEntity.ChannelType;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface exposing all analytics pipeline metrics:
 * <ul>
 *   <li>Delivery Rate</li>
 *   <li>Read Rate</li>
 *   <li>Click-Through Rate</li>
 *   <li>Time-to-Delivery</li>
 *   <li>Failure Rate</li>
 *   <li>Opt-Out Rate</li>
 *   <li>Cost Per Notification</li>
 *   <li>Frequency Cap Hits</li>
 * </ul>
 */
public interface AnalyticsService {

    // -------------------------------------------------------------------------
    // Raw row queries
    // -------------------------------------------------------------------------

    List<AnalyticsResponse> getByDate(LocalDate date);

    List<AnalyticsResponse> getByDateRange(LocalDate from, LocalDate to);

    List<AnalyticsResponse> getByChannelAndDateRange(ChannelType channelType, LocalDate from, LocalDate to);

    AnalyticsResponse getByChannelAndEventTypeAndDate(ChannelType channelType, String eventType, LocalDate date);

    // -------------------------------------------------------------------------
    // Dashboard summary (all 8 metrics, cross-channel)
    // -------------------------------------------------------------------------

    /**
     * Returns the full analytics dashboard summary for a date range.
     * Aggregates all 8 pipeline metrics with per-channel breakdowns and daily trends.
     */
    AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to);

    // -------------------------------------------------------------------------
    // Individual metric APIs
    // -------------------------------------------------------------------------

    /** Delivery rate = totalDelivered / totalSent. Per-channel or all channels. */
    double getDeliveryRate(LocalDate from, LocalDate to, ChannelType channelType);

    /** Read rate = totalRead / totalDelivered. Per-channel or all channels. */
    double getReadRate(LocalDate from, LocalDate to, ChannelType channelType);

    /** Click-through rate = totalClicked / totalSent. Per-channel or all channels. */
    double getClickThroughRate(LocalDate from, LocalDate to, ChannelType channelType);

    /** Failure rate = totalFailed / totalSent. Per-channel or all channels. */
    double getFailureRate(LocalDate from, LocalDate to, ChannelType channelType);

    /** Opt-out rate = totalOptOut / totalSent (all channels). */
    double getOptOutRate(LocalDate from, LocalDate to);

    /** Average delivery time in ms. Per-channel or all channels. */
    double getAvgTimeToDelivery(LocalDate from, LocalDate to, ChannelType channelType);

    /** Cost per notification in USD. Per-channel or all channels. */
    double getCostPerNotification(LocalDate from, LocalDate to, ChannelType channelType);

    /** Total frequency cap hits (suppressed notifications). Per-channel or all channels. */
    long getFrequencyCapHits(LocalDate from, LocalDate to, ChannelType channelType);

    // -------------------------------------------------------------------------
    // Rollup (called by scheduler)
    // -------------------------------------------------------------------------

    /** Full daily rollup — recalculates all derived metrics for the given date. */
    void rollupForDate(LocalDate date);
}
