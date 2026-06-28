package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event record persisted for audit trail and replay capability.
 */
@Entity
@Table(name = "notification_events", indexes = {
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_event_user_id", columnList = "user_id"),
        @Index(name = "idx_event_source", columnList = "source"),
        @Index(name = "idx_event_created_at", columnList = "created_at"),
        @Index(name = "idx_event_processed", columnList = "processed")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "processed")
    @Builder.Default
    private boolean processed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    @Builder.Default
    private EventPriority priority = EventPriority.NORMAL;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum EventPriority {
        LOW, NORMAL, HIGH, CRITICAL
    }
}
