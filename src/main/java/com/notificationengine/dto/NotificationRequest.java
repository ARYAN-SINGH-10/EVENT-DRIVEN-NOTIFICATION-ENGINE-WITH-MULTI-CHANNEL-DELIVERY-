package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for sending a notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request payload for creating and sending a notification")
public class NotificationRequest {

    @Schema(description = "Existing Notification ID if created early in the pipeline")
    private UUID notificationId;

    @NotBlank(message = "userId is required")
    @Size(max = 100)
    @Schema(description = "Target user identifier", example = "user-123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @NotBlank(message = "recipientAddress is required")
    @Size(max = 500)
    @Schema(description = "Recipient address (email, phone, device token, etc.)", example = "user@example.com")
    private String recipientAddress;

    @NotNull(message = "channelType is required")
    @Schema(description = "Delivery channel", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    private ChannelType channelType;

    @Size(max = 500)
    @Schema(description = "Notification subject (for EMAIL, SLACK)")
    private String subject;

    @Schema(description = "Notification body (overrides template if provided)")
    private String body;

    @Schema(description = "Template ID to use for rendering the body")
    private UUID templateId;

    @Schema(description = "Template name (alternative to templateId)")
    private String templateName;

    @Schema(description = "Key-value pairs for template variable substitution")
    private Map<String, String> templateVariables;

    @Schema(description = "Event type triggering this notification", example = "ORDER_SHIPPED")
    private String eventType;

    @Pattern(regexp = "LOW|NORMAL|HIGH|CRITICAL", message = "priority must be LOW, NORMAL, HIGH or CRITICAL")
    @Schema(description = "Delivery priority", example = "NORMAL")
    @Builder.Default
    private String priority = "NORMAL";

    @Min(0) @Max(10)
    @Schema(description = "Maximum retry attempts", example = "3")
    private int maxRetries;

    @Schema(description = "Schedule delivery for this time (null = send immediately)")
    private LocalDateTime scheduledAt;

    @Schema(description = "Notification expiry time")
    private LocalDateTime expiresAt;

    @Schema(description = "Additional metadata as JSON string")
    private String metadata;

    @Schema(description = "Correlation ID for tracing")
    private String correlationId;
}
