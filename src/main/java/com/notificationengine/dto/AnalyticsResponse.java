package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Full analytics response for a channel + event type + date combination.
 * Includes all 8 pipeline metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated notification analytics for a channel and date")
public class AnalyticsResponse {

    private UUID id;
    private ChannelType channelType;
    private String eventType;
    private LocalDate analyticsDate;

    // --- Volume ---
    private long totalSent;
    private long totalDelivered;
    private long totalFailed;
    private long totalPending;
    private long totalRetried;
    private long totalCancelled;
    private long totalRead;
    private long totalClicked;
    private long totalOptOut;
    private long totalCapHits;

    // --- Rates (0.0 – 1.0) ---
    @Schema(description = "Delivered / Sent")
    private Double deliveryRate;

    @Schema(description = "Failed / Sent")
    private Double failureRate;

    @Schema(description = "Read / Delivered")
    private Double readRate;

    @Schema(description = "Clicked / Sent")
    private Double clickThroughRate;

    @Schema(description = "Opted-out / Sent")
    private Double optOutRate;

    // --- Timing ---
    @Schema(description = "Average end-to-end delivery time in milliseconds")
    private Double avgDeliveryTimeMs;

    @Schema(description = "95th-percentile delivery time in milliseconds")
    private Double p95DeliveryTimeMs;

    // --- Cost ---
    @Schema(description = "Total estimated cost in USD for the period")
    private Double estimatedCostUsd;

    @Schema(description = "Average cost per notification in USD")
    private Double costPerNotification;
}
