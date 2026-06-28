package com.notificationengine.repository;

import com.notificationengine.entity.NotificationEvent;
import com.notificationengine.entity.NotificationEvent.EventPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link NotificationEvent}.
 */
@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID> {

    Page<NotificationEvent> findByUserId(String userId, Pageable pageable);

    Page<NotificationEvent> findByEventType(String eventType, Pageable pageable);

    List<NotificationEvent> findByProcessedFalseOrderByCreatedAtAsc();

    @Query("SELECT e FROM NotificationEvent e WHERE e.processed = false " +
           "AND e.priority = :priority ORDER BY e.createdAt ASC")
    List<NotificationEvent> findUnprocessedByPriority(@Param("priority") EventPriority priority);

    Optional<NotificationEvent> findByCorrelationId(String correlationId);

    @Modifying
    @Query("UPDATE NotificationEvent e SET e.processed = true, e.processedAt = :now, " +
           "e.notificationId = :notificationId WHERE e.id = :id")
    int markAsProcessed(@Param("id") UUID id,
                        @Param("now") LocalDateTime now,
                        @Param("notificationId") UUID notificationId);

    @Query("SELECT COUNT(e) FROM NotificationEvent e WHERE e.eventType = :eventType " +
           "AND e.createdAt BETWEEN :from AND :to")
    long countByEventTypeAndDateRange(
            @Param("eventType") String eventType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<NotificationEvent> findByCreatedAtBeforeAndProcessedTrue(LocalDateTime before, Pageable pageable);
}
