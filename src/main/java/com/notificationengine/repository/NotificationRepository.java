package com.notificationengine.repository;

import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;
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
 * Repository for {@link NotificationEntity} with custom query methods.
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByUserId(String userId, Pageable pageable);

    Page<NotificationEntity> findByStatus(NotificationStatus status, Pageable pageable);

    long countByStatus(NotificationStatus status);

    Page<NotificationEntity> findByChannelType(ChannelType channelType, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndStatus(String userId, NotificationStatus status, Pageable pageable);

    List<NotificationEntity> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);

    @Query("SELECT n FROM NotificationEntity n WHERE n.status = 'PENDING' AND " +
           "(n.scheduledAt IS NULL OR n.scheduledAt <= :now) ORDER BY n.createdAt ASC")
    List<NotificationEntity> findPendingNotificationsReadyToSend(@Param("now") LocalDateTime now);

    @Query("SELECT n FROM NotificationEntity n WHERE n.status = 'PENDING' AND " +
           "(n.scheduledAt IS NULL OR n.scheduledAt <= :now) ORDER BY n.createdAt ASC")
    List<NotificationEntity> findPendingNotificationsReadyToSend(
            @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.status = 'RETRYING' AND " +
           "n.scheduledAt IS NOT NULL AND n.scheduledAt <= :now " +
           "AND n.retryCount < n.maxRetries ORDER BY n.scheduledAt ASC")
    List<NotificationEntity> findRetryingNotificationsReadyToSend(@Param("now") LocalDateTime now);

    @Query("SELECT n FROM NotificationEntity n WHERE n.expiresAt IS NOT NULL AND " +
           "n.expiresAt < :now AND n.status IN ('PENDING', 'QUEUED')")
    List<NotificationEntity> findExpiredNotifications(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.userId = :userId AND n.status = :status")
    long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") NotificationStatus status);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.channelType = :channelType " +
           "AND n.createdAt BETWEEN :from AND :to")
    long countByChannelTypeAndDateRange(
            @Param("channelType") ChannelType channelType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // --- Frequency Capping Queries ---
    long countByUserIdAndCreatedAtBetween(String userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndChannelTypeAndCreatedAtBetween(String userId, ChannelType channelType, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndEventTypeAndCreatedAtBetween(String userId, String eventType, LocalDateTime start, LocalDateTime end);

    Optional<NotificationEntity> findTopByUserIdAndEventTypeOrderByCreatedAtDesc(String userId, String eventType);
    // --------------------------------

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.status = :status, n.updatedAt = :now WHERE n.id = :id")
    int updateStatus(@Param("id") UUID id,
                     @Param("status") NotificationStatus status,
                     @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.status = 'EXPIRED', n.updatedAt = :now " +
           "WHERE n.expiresAt < :now AND n.status IN ('PENDING', 'QUEUED')")
    int expireStaleNotifications(@Param("now") LocalDateTime now);

    Optional<NotificationEntity> findByExternalMessageId(String externalMessageId);

    @Query("SELECT n FROM NotificationEntity n WHERE n.createdAt < :before AND n.status = :status")
    List<NotificationEntity> findOldNotifications(
            @Param("before") LocalDateTime before,
            @Param("status") NotificationStatus status,
            Pageable pageable);
}
