package com.notificationengine.service;

import com.notificationengine.dto.NotificationRequest;
import com.notificationengine.dto.NotificationResponse;
import com.notificationengine.dto.PagedResponse;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for notification lifecycle management.
 */
public interface NotificationService {

    NotificationResponse send(NotificationRequest request);

    PagedResponse<NotificationResponse> getAll(Pageable pageable);

    NotificationResponse getById(UUID id);

    PagedResponse<NotificationResponse> getByUserId(String userId, Pageable pageable);

    PagedResponse<NotificationResponse> getByStatus(NotificationStatus status, Pageable pageable);

    PagedResponse<NotificationResponse> getByChannelType(ChannelType channelType, Pageable pageable);

    NotificationResponse cancel(UUID id);

    int processPendingNotifications();

    int processRetryingNotifications();

    int expireStaleNotifications();

    long countByUserIdAndStatus(String userId, NotificationStatus status);

    /**
     * Re-dispatches an existing {@link com.notificationengine.entity.NotificationEntity}
     * directly to the channel, bypassing ingestion checks.
     * Used by RetryScheduler and DLQ replay.
     */
    void reDispatch(UUID notificationId);
}
