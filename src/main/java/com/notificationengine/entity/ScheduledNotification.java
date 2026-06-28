package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Scheduled notification entity for deferred and recurring notification delivery.
 */
@Entity
@Table(name = "scheduled_notifications", indexes = {
        @Index(name = "idx_sched_user_id", columnList = "user_id"),
        @Index(name = "idx_sched_status", columnList = "status"),
        @Index(name = "idx_sched_scheduled_at", columnList = "scheduled_at"),
        @Index(name = "idx_sched_next_run", columnList = "next_run_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private NotificationEntity.ChannelType channelType;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "template_variables", columnDefinition = "TEXT")
    private String templateVariables;

    @Column(name = "recipient_address", nullable = false)
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.SCHEDULED;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    /** Cron expression for recurring notifications (null = one-shot) */
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "recurrence_count")
    @Builder.Default
    private int recurrenceCount = 0;

    @Column(name = "max_recurrences")
    @Builder.Default
    private int maxRecurrences = 1;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ScheduleStatus {
        SCHEDULED, RUNNING, COMPLETED, CANCELLED, FAILED, PAUSED
    }
}
