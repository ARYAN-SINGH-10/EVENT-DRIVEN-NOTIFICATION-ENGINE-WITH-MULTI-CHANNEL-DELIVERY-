package com.notificationengine.service.impl;

import com.notificationengine.analytics.ChannelCostConfig;
import com.notificationengine.dto.AnalyticsResponse;
import com.notificationengine.dto.AnalyticsSummaryResponse;
import com.notificationengine.dto.DailyMetricPoint;
import com.notificationengine.entity.NotificationAnalytics;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.exception.ErrorCode;
import com.notificationengine.exception.NotificationException;
import com.notificationengine.repository.NotificationAnalyticsRepository;
import com.notificationengine.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analytics pipeline service implementing all 8 metrics using JPQL aggregation queries.
 *
 * <p>Metrics computed:
 * <ol>
 *   <li>Delivery Rate       = totalDelivered / totalSent</li>
 *   <li>Read Rate           = totalRead / totalDelivered</li>
 *   <li>Click-Through Rate  = totalClicked / totalSent</li>
 *   <li>Failure Rate        = totalFailed / totalSent</li>
 *   <li>Opt-Out Rate        = totalOptOut / totalSent</li>
 *   <li>Time To Delivery    = avg(avgDeliveryTimeMs)</li>
 *   <li>Cost Per Notification = estimatedCostUsd / totalSent</li>
 *   <li>Frequency Cap Hits  = sum(totalCapHits)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final NotificationAnalyticsRepository analyticsRepository;
    private final ChannelCostConfig costConfig;

    // =========================================================================
    // Raw row queries
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "'date:' + #date")
    public List<AnalyticsResponse> getByDate(LocalDate date) {
        return analyticsRepository.findByAnalyticsDate(date)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "'range:' + #from + ':' + #to")
    public List<AnalyticsResponse> getByDateRange(LocalDate from, LocalDate to) {
        return analyticsRepository.findByDateRange(from, to)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyticsResponse> getByChannelAndDateRange(ChannelType channelType, LocalDate from, LocalDate to) {
        return analyticsRepository.findByChannelTypeAndAnalyticsDateBetween(channelType, from, to)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse getByChannelAndEventTypeAndDate(ChannelType channelType, String eventType, LocalDate date) {
        return analyticsRepository.findByChannelTypeAndEventTypeAndAnalyticsDate(channelType, eventType, date)
                .map(this::toResponse)
                .orElseThrow(() -> new NotificationException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Analytics not found for channel=" + channelType + " eventType=" + eventType + " date=" + date, 404));
    }

    // =========================================================================
    // Dashboard Summary
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to) {
        long totalSent      = coalesce(analyticsRepository.sumTotalSent(from, to));
        long totalDelivered = coalesce(analyticsRepository.sumTotalDelivered(from, to));
        long totalFailed    = coalesce(analyticsRepository.sumTotalFailed(from, to));
        long totalRead      = coalesce(analyticsRepository.sumTotalRead(from, to));
        long totalClicked   = coalesce(analyticsRepository.sumTotalClicked(from, to));
        long totalOptOut    = coalesce(analyticsRepository.sumTotalOptOut(from, to));
        long totalCapHits   = coalesce(analyticsRepository.sumTotalCapHits(from, to));
        Double avgDelivery  = analyticsRepository.avgDeliveryTimeMs(from, to);
        Double totalCost    = nullableDouble(analyticsRepository.sumEstimatedCost(from, to));

        double deliveryRate    = rate(totalDelivered, totalSent);
        double failureRate     = rate(totalFailed, totalSent);
        double readRate        = rate(totalRead, totalDelivered);
        double clickThroughRate = rate(totalClicked, totalSent);
        double optOutRate      = rate(totalOptOut, totalSent);
        double costPerNotif    = totalSent > 0 && totalCost != null ? totalCost / totalSent : 0.0;

        // Per-channel cost and cap hits breakdown
        Map<String, Double> costByChannel = new LinkedHashMap<>();
        Map<String, Long> capHitsByChannel = new LinkedHashMap<>();
        for (ChannelType ch : ChannelType.values()) {
            Double chCost = nullableDouble(analyticsRepository.sumEstimatedCostByChannel(ch, from, to));
            if (chCost != null && chCost > 0) costByChannel.put(ch.name(), chCost);

            long chCaps = coalesce(analyticsRepository.sumTotalCapHitsByChannel(ch, from, to));
            if (chCaps > 0) capHitsByChannel.put(ch.name(), chCaps);
        }

        // Daily trend for delivery rate
        List<DailyMetricPoint> deliveryTrend = buildDailyTrend(from, to, "delivery");
        List<DailyMetricPoint> failureTrend  = buildDailyTrend(from, to, "failure");

        return AnalyticsSummaryResponse.builder()
                .from(from).to(to)
                .totalSent(totalSent).totalDelivered(totalDelivered).totalFailed(totalFailed)
                .totalRead(totalRead).totalClicked(totalClicked)
                .totalOptOut(totalOptOut).totalCapHits(totalCapHits)
                .deliveryRate(deliveryRate).failureRate(failureRate)
                .readRate(readRate).clickThroughRate(clickThroughRate).optOutRate(optOutRate)
                .avgDeliveryTimeMs(avgDelivery)
                .totalCostUsd(totalCost).avgCostPerNotification(costPerNotif)
                .costByChannel(costByChannel).capHitsByChannel(capHitsByChannel)
                .deliveryRateTrend(deliveryTrend).failureRateTrend(failureTrend)
                .build();
    }

    // =========================================================================
    // Individual Metric APIs
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public double getDeliveryRate(LocalDate from, LocalDate to, ChannelType channelType) {
        if (channelType != null) {
            return rate(
                coalesce(analyticsRepository.sumTotalDeliveredByChannelAndDateRange(channelType, from, to)),
                coalesce(analyticsRepository.sumTotalSentByChannelAndDateRange(channelType, from, to)));
        }
        return rate(coalesce(analyticsRepository.sumTotalDelivered(from, to)),
                    coalesce(analyticsRepository.sumTotalSent(from, to)));
    }

    @Override
    @Transactional(readOnly = true)
    public double getReadRate(LocalDate from, LocalDate to, ChannelType channelType) {
        long read = channelType != null
                ? coalesce(analyticsRepository.sumTotalReadByChannel(channelType, from, to))
                : coalesce(analyticsRepository.sumTotalRead(from, to));
        long delivered = channelType != null
                ? coalesce(analyticsRepository.sumTotalDeliveredByChannelAndDateRange(channelType, from, to))
                : coalesce(analyticsRepository.sumTotalDelivered(from, to));
        return rate(read, delivered);
    }

    @Override
    @Transactional(readOnly = true)
    public double getClickThroughRate(LocalDate from, LocalDate to, ChannelType channelType) {
        long clicked = channelType != null
                ? coalesce(analyticsRepository.sumTotalClickedByChannel(channelType, from, to))
                : coalesce(analyticsRepository.sumTotalClicked(from, to));
        long sent = channelType != null
                ? coalesce(analyticsRepository.sumTotalSentByChannelAndDateRange(channelType, from, to))
                : coalesce(analyticsRepository.sumTotalSent(from, to));
        return rate(clicked, sent);
    }

    @Override
    @Transactional(readOnly = true)
    public double getFailureRate(LocalDate from, LocalDate to, ChannelType channelType) {
        long failed = channelType != null
                ? coalesce(analyticsRepository.sumTotalFailedByChannel(channelType, from, to))
                : coalesce(analyticsRepository.sumTotalFailed(from, to));
        long sent = channelType != null
                ? coalesce(analyticsRepository.sumTotalSentByChannelAndDateRange(channelType, from, to))
                : coalesce(analyticsRepository.sumTotalSent(from, to));
        return rate(failed, sent);
    }

    @Override
    @Transactional(readOnly = true)
    public double getOptOutRate(LocalDate from, LocalDate to) {
        return rate(coalesce(analyticsRepository.sumTotalOptOut(from, to)),
                    coalesce(analyticsRepository.sumTotalSent(from, to)));
    }

    @Override
    @Transactional(readOnly = true)
    public double getAvgTimeToDelivery(LocalDate from, LocalDate to, ChannelType channelType) {
        Double avg = channelType != null
                ? analyticsRepository.avgDeliveryTimeMsByChannel(channelType, from, to)
                : analyticsRepository.avgDeliveryTimeMs(from, to);
        return avg != null ? avg : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public double getCostPerNotification(LocalDate from, LocalDate to, ChannelType channelType) {
        Double totalCost = channelType != null
                ? nullableDouble(analyticsRepository.sumEstimatedCostByChannel(channelType, from, to))
                : nullableDouble(analyticsRepository.sumEstimatedCost(from, to));
        long sent = channelType != null
                ? coalesce(analyticsRepository.sumTotalSentByChannelAndDateRange(channelType, from, to))
                : coalesce(analyticsRepository.sumTotalSent(from, to));
        if (sent == 0 || totalCost == null) return 0.0;
        return totalCost / sent;
    }

    @Override
    @Transactional(readOnly = true)
    public long getFrequencyCapHits(LocalDate from, LocalDate to, ChannelType channelType) {
        return channelType != null
                ? coalesce(analyticsRepository.sumTotalCapHitsByChannel(channelType, from, to))
                : coalesce(analyticsRepository.sumTotalCapHits(from, to));
    }

    // =========================================================================
    // Rollup (full daily recompute)
    // =========================================================================

    @Override
    @Transactional
    public void rollupForDate(LocalDate date) {
        log.info("[ANALYTICS-ROLLUP] Starting rollup for date={}", date);
        List<NotificationAnalytics> rows = analyticsRepository.findByAnalyticsDate(date);

        for (NotificationAnalytics a : rows) {
            // Recompute all derived rates
            long sent      = a.getTotalSent();
            long delivered = a.getTotalDelivered();
            long failed    = a.getTotalFailed();
            long read      = a.getTotalRead();
            long clicked   = a.getTotalClicked();
            long optOut    = a.getTotalOptOut();

            a.setDeliveryRate(safeRate(delivered, sent));
            a.setFailureRate(safeRate(failed, sent));
            a.setReadRate(safeRate(read, delivered));
            a.setClickThroughRate(safeRate(clicked, sent));
            a.setOptOutRate(safeRate(optOut, sent));

            // Recompute cost
            double costPerUnit = costConfig.getCostPerUnit(a.getChannelType());
            double estimatedCost = sent * costPerUnit;
            a.setEstimatedCostUsd(estimatedCost);

            analyticsRepository.save(a);
        }

        log.info("[ANALYTICS-ROLLUP] Completed rollup for date={}, rows processed={}", date, rows.size());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<DailyMetricPoint> buildDailyTrend(LocalDate from, LocalDate to, String metric) {
        List<NotificationAnalytics> rows = analyticsRepository.findByDateRange(from, to);

        // Group by date and aggregate across channels
        return rows.stream()
                .collect(Collectors.groupingBy(NotificationAnalytics::getAnalyticsDate))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<NotificationAnalytics> dayRows = entry.getValue();
                    long sent = dayRows.stream().mapToLong(NotificationAnalytics::getTotalSent).sum();
                    long num;
                    if ("delivery".equals(metric)) {
                        num = dayRows.stream().mapToLong(NotificationAnalytics::getTotalDelivered).sum();
                    } else {
                        num = dayRows.stream().mapToLong(NotificationAnalytics::getTotalFailed).sum();
                    }
                    return DailyMetricPoint.builder()
                            .date(date).numerator(num).denominator(sent)
                            .value(safeRate(num, sent))
                            .build();
                })
                .toList();
    }

    private AnalyticsResponse toResponse(NotificationAnalytics a) {
        long sent = a.getTotalSent();
        double costPerUnit = costConfig.getCostPerUnit(a.getChannelType());
        Double estimatedCost = a.getEstimatedCostUsd() != null ? a.getEstimatedCostUsd() : sent * costPerUnit;
        Double costPerNotif = sent > 0 ? estimatedCost / sent : 0.0;

        return AnalyticsResponse.builder()
                .id(a.getId())
                .channelType(a.getChannelType())
                .eventType(a.getEventType())
                .analyticsDate(a.getAnalyticsDate())
                .totalSent(a.getTotalSent())
                .totalDelivered(a.getTotalDelivered())
                .totalFailed(a.getTotalFailed())
                .totalPending(a.getTotalPending())
                .totalRetried(a.getTotalRetried())
                .totalCancelled(a.getTotalCancelled())
                .totalRead(a.getTotalRead())
                .totalClicked(a.getTotalClicked())
                .totalOptOut(a.getTotalOptOut())
                .totalCapHits(a.getTotalCapHits())
                .deliveryRate(a.getDeliveryRate())
                .failureRate(a.getFailureRate())
                .readRate(a.getReadRate())
                .clickThroughRate(a.getClickThroughRate())
                .optOutRate(a.getOptOutRate())
                .avgDeliveryTimeMs(a.getAvgDeliveryTimeMs())
                .p95DeliveryTimeMs(a.getP95DeliveryTimeMs())
                .estimatedCostUsd(estimatedCost)
                .costPerNotification(costPerNotif)
                .build();
    }

    private static double rate(long numerator, long denominator) {
        return denominator > 0 ? (double) numerator / denominator : 0.0;
    }

    private static double safeRate(long numerator, long denominator) {
        return denominator > 0 ? Math.min(1.0, (double) numerator / denominator) : 0.0;
    }

    private static long coalesce(Long value) {
        return value != null ? value : 0L;
    }

    private static Double nullableDouble(Double value) {
        return (value != null && value > 0) ? value : null;
    }
}
