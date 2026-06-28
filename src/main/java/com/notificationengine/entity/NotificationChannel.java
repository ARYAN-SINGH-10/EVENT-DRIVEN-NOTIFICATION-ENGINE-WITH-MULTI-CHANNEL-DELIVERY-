package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Channel definition entity — configuration record for each delivery channel.
 */
@Entity
@Table(name = "notification_channels", indexes = {
        @Index(name = "idx_channel_type", columnList = "channel_type"),
        @Index(name = "idx_channel_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, unique = true, length = 20)
    private NotificationEntity.ChannelType channelType;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "simulation_mode")
    @Builder.Default
    private boolean simulationMode = true;

    /** JSON-serialised provider config (API keys, endpoints, etc.) */
    @Column(name = "provider_config", columnDefinition = "TEXT")
    private String providerConfig;

    @Column(name = "rate_limit_per_minute")
    @Builder.Default
    private int rateLimitPerMinute = 1000;

    @Column(name = "max_payload_size_bytes")
    @Builder.Default
    private int maxPayloadSizeBytes = 65536;

    @Column(name = "supports_html")
    @Builder.Default
    private boolean supportsHtml = false;

    @Column(name = "supports_attachments")
    @Builder.Default
    private boolean supportsAttachments = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
