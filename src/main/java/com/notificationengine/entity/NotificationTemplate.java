package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reusable notification template with dynamic variable substitution support.
 */
@Entity
@Table(name = "notification_templates", indexes = {
        @Index(name = "idx_template_name", columnList = "name"),
        @Index(name = "idx_template_channel", columnList = "channel_type"),
        @Index(name = "idx_template_event_type", columnList = "event_type"),
        @Index(name = "idx_template_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private NotificationEntity.ChannelType channelType;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "subject_template", length = 500)
    private String subjectTemplate;

    @Column(name = "body_template", columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    @Column(name = "html_body_template", columnDefinition = "TEXT")
    private String htmlBodyTemplate;

    @Column(name = "available_variables", columnDefinition = "TEXT")
    private String availableVariables;

    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "en-US";

    @Column(name = "version")
    @Builder.Default
    private int version = 1;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
