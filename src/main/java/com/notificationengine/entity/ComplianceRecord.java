package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * GDPR/compliance record tracking user consent for notification channels.
 */
@Entity
@Table(name = "compliance_records", indexes = {
        @Index(name = "idx_compliance_user_id", columnList = "user_id"),
        @Index(name = "idx_compliance_type", columnList = "consent_type"),
        @Index(name = "idx_compliance_status", columnList = "consent_status"),
        @Index(name = "idx_compliance_user_type", columnList = "user_id, consent_type")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false, length = 20)
    @Builder.Default
    private ConsentStatus consentStatus = ConsentStatus.PENDING;

    @Column(name = "channel_type", length = 20)
    private String channelType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "consent_version", length = 20)
    private String consentVersion;

    @Column(name = "consent_given_at")
    private LocalDateTime consentGivenAt;

    @Column(name = "consent_revoked_at")
    private LocalDateTime consentRevokedAt;

    @Column(name = "data_retention_until")
    private LocalDateTime dataRetentionUntil;

    @Column(name = "deletion_requested_at")
    private LocalDateTime deletionRequestedAt;

    @Column(name = "deletion_completed_at")
    private LocalDateTime deletionCompletedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ConsentType {
        MARKETING, TRANSACTIONAL, ANALYTICS, ALL_COMMUNICATIONS, DATA_RETENTION, WHATSAPP_EXPLICIT
    }

    public enum ConsentStatus {
        PENDING, GRANTED, REVOKED, EXPIRED, DELETION_REQUESTED
    }
}
