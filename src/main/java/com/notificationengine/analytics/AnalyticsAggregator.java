package com.notificationengine.analytics;

import com.notificationengine.entity.NotificationAnalytics;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.repository.NotificationAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Aggregates notification delivery metrics into the analytics table.
 *
 * <p>Tracks all 8 pipeline metrics:
 * Delivery Rate, Read Rate, Click-Through Rate, Time-to-Delivery,
 * Failure Rate, Opt-Out Rate, Cost Per Notification, Frequency Cap Hits.
 *
 * <p>Called on individual notification status changes for real-time approximate metrics,
 * and daily by {@code AnalyticsRollupScheduler} for exact recalculation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsAggregator {

    private final NotificationAnalyticsRepository analyticsRepository;
    private final ChannelCostConfig costConfig;

    /**
     * Records a notification delivery event and updates the daily analytics row.
     *
     * @param channelType  delivery channel
     * @param eventType    event type (e.g. "TXNX-001")
     * @param status       new notification status
     * @param deliveryMs   end-to-end delivery time in ms (non-null only for DELIVERED)
     */
    @Async("analyticsExecutor")
    @Transactional
    public void recordDeliveryEvent(ChannelType channelType, String eventType,
                                    NotificationStatus status, Long deliveryMs) {
        if (channelType == null) return;
        LocalDate today = LocalDate.now();

        Optional<NotificationAnalytics> existing =
                analyticsRepository.findByChannelTypeAndEventTypeAndAnalyticsDate(channelType, eventType, today);

        NotificationAnalytics analytics = existing.orElseGet(() ->
                NotificationAnalytics.builder()
                        .channelType(channelType)
                        .eventType(eventType)
                        .analyticsDate(today)
                        .build());

        // Increment volume counter for the given status
        switch (status) {
            case SENT       -> analytics.setTotalSent(analytics.getTotalSent() + 1);
            case DELIVERED  -> analytics.setTotalDelivered(analytics.getTotalDelivered() + 1);
            case FAILED     -> analytics.setTotalFailed(analytics.getTotalFailed() + 1);
            case PENDING    -> analytics.setTotalPending(analytics.getTotalPending() + 1);
            case CANCELLED  -> analytics.setTotalCancelled(analytics.getTotalCancelled() + 1);
            case RETRYING   -> analytics.setTotalRetried(analytics.getTotalRetried() + 1);
            case READ       -> analytics.setTotalRead(analytics.getTotalRead() + 1);
            case CAPPED     -> analytics.setTotalCapHits(analytics.getTotalCapHits() + 1);
            default         -> {}
        }

        // Recompute derived rates
        long sent      = analytics.getTotalSent();
        long delivered = analytics.getTotalDelivered();
        long failed    = analytics.getTotalFailed();
        long read      = analytics.getTotalRead();
        long clicked   = analytics.getTotalClicked();
        long optOut    = analytics.getTotalOptOut();

        if (sent > 0) {
            analytics.setDeliveryRate((double) delivered / sent);
            analytics.setFailureRate((double) failed / sent);
            analytics.setClickThroughRate((double) clicked / sent);
            analytics.setOptOutRate((double) optOut / sent);
        }
        if (delivered > 0) {
            analytics.setReadRate((double) read / delivered);
        }

        // Update rolling average delivery time (only on DELIVERED events)
        if (deliveryMs != null && status == NotificationStatus.DELIVERED) {
            double currentAvg = analytics.getAvgDeliveryTimeMs() != null ? analytics.getAvgDeliveryTimeMs() : 0;
            double newAvg = delivered > 1
                    ? (currentAvg * (delivered - 1) + deliveryMs) / delivered
                    : deliveryMs;
            analytics.setAvgDeliveryTimeMs(newAvg);
        }

        // Recompute estimated cost (cost per unit × total sent for the day)
        if (sent > 0) {
            double costPerUnit = costConfig.getCostPerUnit(channelType);
            analytics.setEstimatedCostUsd(sent * costPerUnit);
        }

        analyticsRepository.save(analytics);
        log.debug("[ANALYTICS] Recorded: channel={} eventType={} status={}", channelType, eventType, status);
    }

    /**
     * Records an opt-out event (user disabled a channel for an event type).
     */
    @Async("analyticsExecutor")
    @Transactional
    public void recordOptOutEvent(ChannelType channelType, String eventType) {
        if (channelType == null) return;
        LocalDate today = LocalDate.now();

        NotificationAnalytics analytics = analyticsRepository
                .findByChannelTypeAndEventTypeAndAnalyticsDate(channelType, eventType, today)
                .orElseGet(() -> NotificationAnalytics.builder()
                        .channelType(channelType).eventType(eventType).analyticsDate(today).build());

        analytics.setTotalOptOut(analytics.getTotalOptOut() + 1);
        long sent = analytics.getTotalSent();
        if (sent > 0) {
            analytics.setOptOutRate((double) analytics.getTotalOptOut() / sent);
        }
        analyticsRepository.save(analytics);
    }

    /**
     * Records a click event (user clicked a link in the notification).
     */
    @Async("analyticsExecutor")
    @Transactional
    public void recordClickEvent(ChannelType channelType, String eventType) {
        if (channelType == null) return;
        LocalDate today = LocalDate.now();

        NotificationAnalytics analytics = analyticsRepository
                .findByChannelTypeAndEventTypeAndAnalyticsDate(channelType, eventType, today)
                .orElseGet(() -> NotificationAnalytics.builder()
                        .channelType(channelType).eventType(eventType).analyticsDate(today).build());

        analytics.setTotalClicked(analytics.getTotalClicked() + 1);
        long sent = analytics.getTotalSent();
        if (sent > 0) {
            analytics.setClickThroughRate((double) analytics.getTotalClicked() / sent);
        }
        analyticsRepository.save(analytics);
    }
}
