package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable snapshot of a {@link NotificationTemplate} at a specific version.
 * Created automatically whenever a template is updated, enabling full version
 * history and rollback capability.
 */
@Entity
@Table(name = "template_versions", indexes = {
        @Index(name = "idx_tv_template_id", columnList = "template_id"),
        @Index(name = "idx_tv_template_version", columnList = "template_id, version")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** FK reference to the live template. */
    @Column(name = "template_id", nullable = false, updatable = false)
    private UUID templateId;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    /** The version number of this snapshot (mirrors NotificationTemplate.version at the time). */
    @Column(name = "version", nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private NotificationEntity.ChannelType channelType;

    @Column(name = "event_type", length = 100)
    private String eventType;

    /** ISO locale code (e.g. "en", "hi", "mr", "ta", "te"). */
    @Column(name = "locale", length = 10)
    private String locale;

    @Column(name = "subject_template", length = 500)
    private String subjectTemplate;

    @Column(name = "body_template", columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    @Column(name = "html_body_template", columnDefinition = "TEXT")
    private String htmlBodyTemplate;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
