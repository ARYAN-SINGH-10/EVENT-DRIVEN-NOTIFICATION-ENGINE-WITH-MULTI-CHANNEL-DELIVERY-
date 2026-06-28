package com.notificationengine.repository;

import com.notificationengine.entity.NotificationStateAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationStateAuditRepository extends JpaRepository<NotificationStateAudit, UUID> {
    List<NotificationStateAudit> findByNotificationIdOrderByCreatedAtAsc(UUID notificationId);
}
