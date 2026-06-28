package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEvent.EventPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Payload for publishing a domain event to the notification system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Event payload for triggering notifications")
public class EventPayload {

    @NotBlank(message = "eventType is required")
    @Schema(description = "Event type identifier", example = "ORDER_SHIPPED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventType;

    @NotBlank(message = "userId is required")
    @Schema(description = "Target user ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "Source system or service", example = "order-service")
    private String source;

    @Schema(description = "Correlation ID for distributed tracing")
    private String correlationId;

    @Schema(description = "Event data as key-value pairs")
    private Map<String, String> data;

    @Schema(description = "Raw JSON payload (overrides data map)")
    private String rawPayload;

    @Schema(description = "Channel types to target (null = use user preferences)")
    private ChannelType[] targetChannels;

    @Schema(description = "Event processing priority")
    @Builder.Default
    private EventPriority priority = EventPriority.NORMAL;

    @Schema(description = "Additional metadata")
    private String metadata;
}
