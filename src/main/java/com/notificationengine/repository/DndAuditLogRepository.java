package com.notificationengine.repository;

import com.notificationengine.entity.DndAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DndAuditLogRepository extends JpaRepository<DndAuditLog, UUID> {
}
