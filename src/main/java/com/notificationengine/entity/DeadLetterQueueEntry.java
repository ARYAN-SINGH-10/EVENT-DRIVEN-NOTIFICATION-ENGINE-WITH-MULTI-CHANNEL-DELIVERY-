package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dead Letter Queue entry for failed notifications requiring retry or manual intervention.
 */
@Entity
@Table(name = "dead_letter_queue", indexes = {
        @Index(name = "idx_dlq_notification_id", columnList = "notification_id"),
        @Index(name = "idx_dlq_status", columnList = "status"),
        @Index(name = "idx_dlq_next_retry", columnList = "next_retry_at"),
        @Index(name = "idx_dlq_channel", columnList = "channel_type")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadLetterQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 20)
    private NotificationEntity.ChannelType channelType;

    @Column(name = "original_payload", columnDefinition = "TEXT")
    private String originalPayload;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "failure_count")
    @Builder.Default
    private int failureCount = 1;

    @Column(name = "max_retry_attempts")
    @Builder.Default
    private int maxRetryAttempts = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private DlqStatus status = DlqStatus.PENDING_RETRY;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DlqStatus {
        PENDING_RETRY, RETRYING, EXHAUSTED, RESOLVED, DISCARDED
    }
}
