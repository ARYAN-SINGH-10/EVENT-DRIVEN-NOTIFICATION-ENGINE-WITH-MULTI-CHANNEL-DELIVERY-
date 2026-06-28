package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores per-user notification channel preferences and quiet-hours settings.
 */
@Entity
@Table(name = "user_preferences", indexes = {
        @Index(name = "idx_pref_user_id", columnList = "user_id"),
        @Index(name = "idx_pref_user_channel", columnList = "user_id, channel_type", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private NotificationEntity.ChannelType channelType;

    @Column(name = "enabled")
    @Builder.Default
    private boolean enabled = true;

    /** Comma-separated event types to opt out of. */
    @Column(name = "opted_out_event_types", columnDefinition = "TEXT")
    private String optedOutEventTypes;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", length = 20)
    @Builder.Default
    private NotificationFrequency frequency = NotificationFrequency.IMMEDIATE;

    /** Quiet hours start (e.g. "22:00") */
    @Column(name = "quiet_hours_start", length = 5)
    private String quietHoursStart;

    /** Quiet hours end (e.g. "07:00") */
    @Column(name = "quiet_hours_end", length = 5)
    private String quietHoursEnd;

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "en-US";

    @Column(name = "priority_override", length = 20)
    private String priorityOverride;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum NotificationFrequency {
        IMMEDIATE, HOURLY, DAILY, WEEKLY, DIGEST
    }
}
