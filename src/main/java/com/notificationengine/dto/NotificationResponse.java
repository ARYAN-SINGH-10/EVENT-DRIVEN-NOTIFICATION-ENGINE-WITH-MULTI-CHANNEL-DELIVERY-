package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Notification response payload")
public class NotificationResponse {

    @Schema(description = "Unique notification identifier")
    private UUID id;

    @Schema(description = "Target user identifier")
    private String userId;

    @Schema(description = "Recipient address")
    private String recipientAddress;

    @Schema(description = "Delivery channel")
    private ChannelType channelType;

    @Schema(description = "Current delivery status")
    private NotificationStatus status;

    @Schema(description = "Notification subject")
    private String subject;

    @Schema(description = "Notification body")
    private String body;

    @Schema(description = "Associated template ID")
    private UUID templateId;

    @Schema(description = "Event type")
    private String eventType;

    @Schema(description = "Delivery priority")
    private String priority;

    @Schema(description = "Number of retry attempts made")
    private int retryCount;

    @Schema(description = "External provider message ID")
    private String externalMessageId;

    @Schema(description = "Scheduled delivery time")
    private LocalDateTime scheduledAt;

    @Schema(description = "Actual send timestamp")
    private LocalDateTime sentAt;

    @Schema(description = "Confirmed delivery timestamp")
    private LocalDateTime deliveredAt;

    @Schema(description = "Expiry timestamp")
    private LocalDateTime expiresAt;

    @Schema(description = "Error details if delivery failed")
    private String errorMessage;

    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
