package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregated delivery analytics per channel, date and event type.
 */
@Entity
@Table(name = "notification_analytics", indexes = {
        @Index(name = "idx_analytics_channel", columnList = "channel_type"),
        @Index(name = "idx_analytics_date", columnList = "analytics_date"),
        @Index(name = "idx_analytics_event_type", columnList = "event_type"),
        @Index(name = "idx_analytics_channel_date", columnList = "channel_type, analytics_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private NotificationEntity.ChannelType channelType;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "analytics_date", nullable = false)
    private java.time.LocalDate analyticsDate;

    @Column(name = "total_sent")
    @Builder.Default
    private long totalSent = 0;

    @Column(name = "total_delivered")
    @Builder.Default
    private long totalDelivered = 0;

    @Column(name = "total_failed")
    @Builder.Default
    private long totalFailed = 0;

    @Column(name = "total_pending")
    @Builder.Default
    private long totalPending = 0;

    @Column(name = "total_retried")
    @Builder.Default
    private long totalRetried = 0;

    @Column(name = "total_cancelled")
    @Builder.Default
    private long totalCancelled = 0;

    @Column(name = "avg_delivery_time_ms")
    private Double avgDeliveryTimeMs;

    @Column(name = "p95_delivery_time_ms")
    private Double p95DeliveryTimeMs;

    @Column(name = "delivery_rate")
    private Double deliveryRate;

    @Column(name = "failure_rate")
    private Double failureRate;

    // --- Extended Metrics ---

    @Column(name = "total_read")
    @Builder.Default
    private long totalRead = 0;

    @Column(name = "total_clicked")
    @Builder.Default
    private long totalClicked = 0;

    @Column(name = "total_opt_out")
    @Builder.Default
    private long totalOptOut = 0;

    /** Notifications blocked by the frequency capping engine. */
    @Column(name = "total_cap_hits")
    @Builder.Default
    private long totalCapHits = 0;

    /** total_read / total_delivered */
    @Column(name = "read_rate")
    private Double readRate;

    /** total_clicked / total_sent */
    @Column(name = "click_through_rate")
    private Double clickThroughRate;

    /** total_opt_out / total_sent */
    @Column(name = "opt_out_rate")
    private Double optOutRate;

    /** Estimated channel delivery cost in USD for the day */
    @Column(name = "estimated_cost_usd")
    private Double estimatedCostUsd;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
