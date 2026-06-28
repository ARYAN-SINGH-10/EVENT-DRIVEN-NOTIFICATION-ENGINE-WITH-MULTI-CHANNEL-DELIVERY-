package com.notificationengine.service.impl;

import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.entity.NotificationStateAudit;
import com.notificationengine.repository.NotificationRepository;
import com.notificationengine.repository.NotificationStateAuditRepository;
import com.notificationengine.service.NotificationStateMachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationStateMachineServiceImpl implements NotificationStateMachineService {

    private final NotificationRepository notificationRepository;
    private final NotificationStateAuditRepository auditRepository;

    // Defines allowed transitions: Key = Current State, Value = Set of allowed Next States
    private static final Map<NotificationStatus, Set<NotificationStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(NotificationStatus.class);

    static {
        // Initial ingest
        ALLOWED_TRANSITIONS.put(NotificationStatus.CREATED, EnumSet.of(NotificationStatus.ROUTED, NotificationStatus.FAILED));
        
        // Routing done, now check compliance or enrich
        ALLOWED_TRANSITIONS.put(NotificationStatus.ROUTED, EnumSet.of(
                NotificationStatus.DND, 
                NotificationStatus.CAPPED,
                NotificationStatus.ENRICHED,
                NotificationStatus.FAILED
        ));
        
        // Personalization done
        ALLOWED_TRANSITIONS.put(NotificationStatus.ENRICHED, EnumSet.of(
                NotificationStatus.QUIET, 
                NotificationStatus.QUEUED, 
                NotificationStatus.PENDING,
                NotificationStatus.SENT,
                NotificationStatus.FAILED
        ));
        
        // Blocked states (terminal or quasi-terminal)
        ALLOWED_TRANSITIONS.put(NotificationStatus.DND, EnumSet.noneOf(NotificationStatus.class));
        ALLOWED_TRANSITIONS.put(NotificationStatus.CAPPED, EnumSet.noneOf(NotificationStatus.class));
        
        // Queued states can go to SENT or EXPIRED/CANCELLED
        ALLOWED_TRANSITIONS.put(NotificationStatus.QUIET, EnumSet.of(NotificationStatus.QUEUED, NotificationStatus.CANCELLED, NotificationStatus.EXPIRED));
        ALLOWED_TRANSITIONS.put(NotificationStatus.QUEUED, EnumSet.of(NotificationStatus.SENT, NotificationStatus.PROCESSING, NotificationStatus.CANCELLED, NotificationStatus.EXPIRED, NotificationStatus.FAILED));
        ALLOWED_TRANSITIONS.put(NotificationStatus.PENDING, EnumSet.of(NotificationStatus.SENT, NotificationStatus.CANCELLED, NotificationStatus.EXPIRED, NotificationStatus.FAILED, NotificationStatus.PROCESSING));
        ALLOWED_TRANSITIONS.put(NotificationStatus.PROCESSING, EnumSet.of(NotificationStatus.SENT, NotificationStatus.FAILED));
        
        // Dispatch states
        ALLOWED_TRANSITIONS.put(NotificationStatus.SENT, EnumSet.of(NotificationStatus.DELIVERED, NotificationStatus.FAILED, NotificationStatus.RETRYING));
        ALLOWED_TRANSITIONS.put(NotificationStatus.DELIVERED, EnumSet.of(NotificationStatus.READ));
        ALLOWED_TRANSITIONS.put(NotificationStatus.READ, EnumSet.noneOf(NotificationStatus.class));
        
        // Failure and Retries
        ALLOWED_TRANSITIONS.put(NotificationStatus.FAILED, EnumSet.of(NotificationStatus.RETRYING, NotificationStatus.DLQ));
        ALLOWED_TRANSITIONS.put(NotificationStatus.RETRYING, EnumSet.of(NotificationStatus.SENT, NotificationStatus.FAILED, NotificationStatus.DLQ));
        ALLOWED_TRANSITIONS.put(NotificationStatus.DLQ, EnumSet.noneOf(NotificationStatus.class));
        
        // Legacy
        ALLOWED_TRANSITIONS.put(NotificationStatus.CANCELLED, EnumSet.noneOf(NotificationStatus.class));
        ALLOWED_TRANSITIONS.put(NotificationStatus.EXPIRED, EnumSet.noneOf(NotificationStatus.class));
    }

    @Override
    @Transactional
    public NotificationEntity transitionState(NotificationEntity notification, NotificationStatus newState, String reason) {
        NotificationStatus previousState = notification.getStatus();

        if (previousState != null && previousState != newState) {
            Set<NotificationStatus> allowed = ALLOWED_TRANSITIONS.get(previousState);
            if (allowed == null || !allowed.contains(newState)) {
                log.warn("[STATE-MACHINE] Invalid transition attempted: {} -> {} for notification {}", previousState, newState, notification.getId());
                throw new IllegalStateException("Invalid state transition from " + previousState + " to " + newState);
            }
        }

        // Update notification
        notification.setStatus(newState);
        NotificationEntity saved = notificationRepository.save(notification);

        // Save audit
        NotificationStateAudit audit = NotificationStateAudit.builder()
                .notificationId(saved.getId())
                .previousState(previousState)
                .newState(newState)
                .reason(reason)
                .build();
        auditRepository.save(audit);

        log.debug("[STATE-MACHINE] Transitioned notification {} from {} to {}. Reason: {}", saved.getId(), previousState, newState, reason);
        return saved;
    }
}
