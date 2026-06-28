package com.notificationengine.entity;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit trail for all compliance and DND evaluations.
 */
@Entity
@Table(name = "dnd_audit_logs", indexes = {
        @Index(name = "idx_dnd_audit_phone", columnList = "phone_number"),
        @Index(name = "idx_dnd_audit_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DndAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 20)
    private ChannelType channelType;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", length = 50)
    private ComplianceAction actionTaken;

    @Column(name = "reason", length = 500)
    private String reason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ComplianceAction {
        ALLOWED_TRANSACTIONAL,
        ALLOWED_PROMOTIONAL,
        BLOCKED_DND,
        BLOCKED_NO_WHATSAPP_CONSENT
    }
}
