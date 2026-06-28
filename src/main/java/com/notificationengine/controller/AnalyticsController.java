package com.notificationengine.controller;

import com.notificationengine.dto.AnalyticsResponse;
import com.notificationengine.dto.AnalyticsSummaryResponse;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Analytics Pipeline REST API — exposes all 8 notification metrics.
 *
 * <p>Metrics available:
 * <ul>
 *   <li>GET /api/v1/analytics/summary      — full dashboard</li>
 *   <li>GET /api/v1/analytics/delivery-rate</li>
 *   <li>GET /api/v1/analytics/read-rate</li>
 *   <li>GET /api/v1/analytics/click-through-rate</li>
 *   <li>GET /api/v1/analytics/failure-rate</li>
 *   <li>GET /api/v1/analytics/opt-out-rate</li>
 *   <li>GET /api/v1/analytics/time-to-delivery</li>
 *   <li>GET /api/v1/analytics/cost-per-notification</li>
 *   <li>GET /api/v1/analytics/frequency-cap-hits</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics API", description = "8-metric notification analytics pipeline")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // =========================================================================
    // Dashboard Summary
    // =========================================================================

    @GetMapping("/summary")
    @Operation(
        summary = "Full analytics dashboard summary",
        description = "Returns all 8 pipeline metrics aggregated across all channels for the given date range, " +
                      "with per-channel breakdowns and daily trend series."
    )
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Start date (inclusive), e.g. 2026-06-01") LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "End date (inclusive), e.g. 2026-06-28") LocalDate to) {
        return ResponseEntity.ok(analyticsService.getSummary(from, to));
    }

    // =========================================================================
    // Raw Row Queries
    // =========================================================================

    @GetMapping("/date/{date}")
    @Operation(summary = "Raw analytics rows for a specific date (all channels and event types)")
    public ResponseEntity<List<AnalyticsResponse>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(analyticsService.getByDate(date));
    }

    @GetMapping("/range")
    @Operation(summary = "Raw analytics rows for a date range")
    public ResponseEntity<List<AnalyticsResponse>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getByDateRange(from, to));
    }

    @GetMapping("/channel/{channelType}/range")
    @Operation(summary = "Raw analytics rows for a specific channel and date range")
    public ResponseEntity<List<AnalyticsResponse>> getByChannelAndDateRange(
            @PathVariable ChannelType channelType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getByChannelAndDateRange(channelType, from, to));
    }

    @GetMapping("/channel/{channelType}/event/{eventType}/date/{date}")
    @Operation(summary = "Single analytics row for channel + event type + date")
    public ResponseEntity<AnalyticsResponse> getByChannelAndEventTypeAndDate(
            @PathVariable ChannelType channelType,
            @PathVariable String eventType,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(analyticsService.getByChannelAndEventTypeAndDate(channelType, eventType, date));
    }

    // =========================================================================
    // Metric 1: Delivery Rate
    // =========================================================================

    @GetMapping("/delivery-rate")
    @Operation(
        summary = "Delivery Rate",
        description = "Percentage of sent notifications that were successfully delivered. " +
                      "Formula: totalDelivered / totalSent. Pass channelType to filter by channel."
    )
    public ResponseEntity<Map<String, Object>> getDeliveryRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ChannelType channelType) {
        double rate = analyticsService.getDeliveryRate(from, to, channelType);
        return ResponseEntity.ok(buildMetricResponse("delivery_rate", rate, from, to, channelType));
    }

    // =========================================================================
    // Metric 2: Read Rate
    // =========================================================================

    @GetMapping("/read-rate")
    @Operation(
        summary = "Read Rate",
        description = "Percentage of delivered notifications that were read by the user. " +
                      "Formula: totalRead / totalDelivered."
    )
    public ResponseEntity<Map<String, Object>> getReadRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ChannelType channelType) {
        double rate = analyticsService.getReadRate(from, to, channelType);
        return ResponseEntity.ok(buildMetricResponse("read_rate", rate, from, to, channelType));
    }

    // =========================================================================
    // Metric 3: Click-Through Rate
    // =========================================================================

    @GetMapping("/click-through-rate")
    @Operation(
        summary = "Click-Through Rate (CTR)",
        description = "Percentage of sent notifications on which the user clicked a link. " +
                      "Formula: totalClicked / totalSent."
    )
    public ResponseEntity<Map<String, Object>> getClickThroughRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ChannelType channelType) {
        double rate = analyticsService.getClickThroughRate(from, to, channelType);
        return ResponseEntity.ok(buildMetricResponse("click_through_rate", rate, from, to, channelType));
    }

    // =========================================================================
    // Metric 4: Failure Rate
    // =========================================================================

    @GetMapping("/failure-rate")
    @Operation(
        summary = "Failure Rate",
        description = "Percentage of sent notifications that failed delivery. " +
                      "Formula: totalFailed / totalSent."
    )
    public ResponseEntity<Map<String, Object>> getFailureRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ChannelType channelType) {
        double rate = analyticsService.getFailureRate(from, to, channelType);
        return ResponseEntity.ok(buildMetricResponse("failure_rate", rate, from, to, channelType));
    }

    // =========================================================================
    // Metric 5: Opt-Out Rate
    // =========================================================================

    @GetMapping("/opt-out-rate")
    @Operation(
        summary = "Opt-Out Rate",
        description = "Percentage of users who opted out of notifications. " +
                      "Formula: totalOptOut / totalSent."
    )
    public ResponseEntity<Map<String, Object>> getOptOutRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        double rate = analyticsService.getOptOutRate(from, to);
        return ResponseEntity.ok(buildMetricResponse("opt_out_rate", rate, from, to, null));
    }

    // =========================================================================
    // Metric 6: Time To Delivery
    // =========================================================================

    @GetMapping("/time-to-delivery")
    @Operation(
        summary = "Average Time To Delivery",
        description = "Average end-to-end notification delivery latency in milliseconds. " +
                      "Pass channelType to see per-channel latency (e.g., SMS vs PUSH)."
    )
    public ResponseEntity<Map<String, Object>> getTimeToDelivery(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ChannelType channelType) {
        double avgMs = analyticsService.getAvgTimeToDelivery(from, to, channelType);
        return ResponseEntity.ok(Map.of(
                "metric", "avg_time_to_delivery_ms",
                "value", avgMs,
                "from", from,
                "to", to,
                "channel", channelType != null ? channelType.name() : "ALL"
        ));
    }

    // =========================================================================
    // Metric 7: Cost Per Notification
    // =========================================================================

    @GetMapping("/cost-per-notification")
    @Operation(
        summary = "Cost Per Notification",
        description = "Average cost in USD per notification sent. " +
                      "Costs are configurable per channel (SMS=$0.0075, Email=$0.001, etc.)."
    )
    public ResponseEntity<Map<String, Object>> getCostPerNotification(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ChannelType channelType) {
        double cost = analyticsService.getCostPerNotification(from, to, channelType);
        return ResponseEntity.ok(Map.of(
                "metric", "cost_per_notification_usd",
                "value", cost,
                "from", from,
                "to", to,
                "channel", channelType != null ? channelType.name() : "ALL"
        ));
    }

    // =========================================================================
    // Metric 8: Frequency Cap Hits
    // =========================================================================

    @GetMapping("/frequency-cap-hits")
    @Operation(
        summary = "Frequency Cap Hits",
        description = "Total number of notifications suppressed by the frequency capping engine. " +
                      "High values indicate users approaching their notification limits."
    )
    public ResponseEntity<Map<String, Object>> getFrequencyCapHits(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ChannelType channelType) {
        long hits = analyticsService.getFrequencyCapHits(from, to, channelType);
        return ResponseEntity.ok(Map.of(
                "metric", "frequency_cap_hits",
                "value", hits,
                "from", from,
                "to", to,
                "channel", channelType != null ? channelType.name() : "ALL"
        ));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private Map<String, Object> buildMetricResponse(
            String metric, double value, LocalDate from, LocalDate to, ChannelType channelType) {
        return Map.of(
                "metric", metric,
                "value", value,
                "value_pct", String.format("%.2f%%", value * 100),
                "from", from,
                "to", to,
                "channel", channelType != null ? channelType.name() : "ALL"
        );
    }
}
