package com.notificationengine.repository;

import com.notificationengine.entity.DeadLetterQueueEntry;
import com.notificationengine.entity.DeadLetterQueueEntry.DlqStatus;
import com.notificationengine.entity.NotificationEntity.ChannelType;
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
 * Repository for {@link DeadLetterQueueEntry}.
 */
@Repository
public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueueEntry, UUID> {

    Page<DeadLetterQueueEntry> findByStatus(DlqStatus status, Pageable pageable);

    Optional<DeadLetterQueueEntry> findByNotificationId(UUID notificationId);

    List<DeadLetterQueueEntry> findByUserIdAndStatus(String userId, DlqStatus status);

    List<DeadLetterQueueEntry> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT d FROM DeadLetterQueueEntry d WHERE d.status = 'PENDING_RETRY' " +
           "AND d.nextRetryAt <= :now AND d.failureCount < d.maxRetryAttempts " +
           "ORDER BY d.nextRetryAt ASC")
    List<DeadLetterQueueEntry> findEntriesReadyForRetry(@Param("now") LocalDateTime now);

    @Query("SELECT d FROM DeadLetterQueueEntry d WHERE d.status = 'PENDING_RETRY' " +
           "AND d.nextRetryAt <= :now AND d.failureCount < d.maxRetryAttempts " +
           "ORDER BY d.nextRetryAt ASC")
    List<DeadLetterQueueEntry> findEntriesReadyForRetry(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT COUNT(d) FROM DeadLetterQueueEntry d WHERE d.status = :status")
    long countByStatus(@Param("status") DlqStatus status);

    @Query("SELECT COUNT(d) FROM DeadLetterQueueEntry d WHERE d.channelType = :channelType " +
           "AND d.status = 'PENDING_RETRY'")
    long countPendingByChannel(@Param("channelType") ChannelType channelType);

    @Modifying
    @Query("UPDATE DeadLetterQueueEntry d SET d.status = :status, d.resolvedAt = :resolvedAt, " +
           "d.resolvedBy = :resolvedBy, d.resolutionNotes = :notes WHERE d.id = :id")
    int resolve(@Param("id") UUID id,
                @Param("status") DlqStatus status,
                @Param("resolvedAt") LocalDateTime resolvedAt,
                @Param("resolvedBy") String resolvedBy,
                @Param("notes") String notes);

    List<DeadLetterQueueEntry> findByFailureCountGreaterThanEqualAndStatus(
            int failureCount, DlqStatus status);
}
