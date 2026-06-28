package com.notificationengine.repository;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.ScheduledNotification;
import com.notificationengine.entity.ScheduledNotification.ScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ScheduledNotification}.
 */
@Repository
public interface ScheduledNotificationRepository extends JpaRepository<ScheduledNotification, UUID> {

    Page<ScheduledNotification> findByUserId(String userId, Pageable pageable);

    Page<ScheduledNotification> findByStatus(ScheduleStatus status, Pageable pageable);

    @Query("SELECT s FROM ScheduledNotification s WHERE s.status = 'SCHEDULED' " +
           "AND s.nextRunAt <= :now ORDER BY s.nextRunAt ASC")
    List<ScheduledNotification> findDueScheduledNotifications(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM ScheduledNotification s WHERE s.status = 'SCHEDULED' " +
           "AND s.nextRunAt <= :now ORDER BY s.nextRunAt ASC")
    List<ScheduledNotification> findDueScheduledNotifications(
            @Param("now") LocalDateTime now, Pageable pageable);

    List<ScheduledNotification> findByUserIdAndStatus(String userId, ScheduleStatus status);

    @Modifying
    @Query("UPDATE ScheduledNotification s SET s.status = :status WHERE s.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") ScheduleStatus status);

    @Query("SELECT COUNT(s) FROM ScheduledNotification s WHERE s.userId = :userId AND s.status = :status")
    long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") ScheduleStatus status);

    List<ScheduledNotification> findByChannelTypeAndStatus(ChannelType channelType, ScheduleStatus status);
}
