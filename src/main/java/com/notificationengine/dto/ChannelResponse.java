package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for a notification channel configuration. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChannelResponse {
    private UUID id;
    private ChannelType channelType;
    private String displayName;
    private String description;
    private boolean active;
    private boolean simulationMode;
    private int rateLimitPerMinute;
    private int maxPayloadSizeBytes;
    private boolean supportsHtml;
    private boolean supportsAttachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
