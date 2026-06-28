package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.DeadLetterQueueEntry.DlqStatus;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for a Dead Letter Queue entry. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DlqResponse {
    private UUID id;
    private UUID notificationId;
    private String userId;
    private ChannelType channelType;
    private String failureReason;
    private int failureCount;
    private int maxRetryAttempts;
    private DlqStatus status;
    private LocalDateTime nextRetryAt;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;
    private LocalDateTime createdAt;
}
