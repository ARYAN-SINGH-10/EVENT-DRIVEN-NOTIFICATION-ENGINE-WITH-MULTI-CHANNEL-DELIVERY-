package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating a notification template.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request payload for creating or updating a notification template")
public class TemplateRequest {

    @NotBlank(message = "name is required")
    @Size(max = 200)
    @Schema(description = "Unique template name", example = "ORDER_SHIPPED_EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 1000)
    @Schema(description = "Human-readable description")
    private String description;

    @NotNull(message = "channelType is required")
    @Schema(description = "Target delivery channel", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    private ChannelType channelType;

    @Schema(description = "Event type this template handles", example = "ORDER_SHIPPED")
    private String eventType;

    @Size(max = 500)
    @Schema(description = "Subject template (supports {{variable}} placeholders)")
    private String subjectTemplate;

    @NotBlank(message = "bodyTemplate is required")
    @Schema(description = "Body template with {{variable}} placeholders", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bodyTemplate;

    @Schema(description = "HTML body template for email channels")
    private String htmlBodyTemplate;

    @Schema(description = "Comma-separated list of available template variables")
    private String availableVariables;

    @Pattern(regexp = "[a-z]{2}-[A-Z]{2}", message = "locale must be in format en-US")
    @Schema(description = "Locale for this template", example = "en-US")
    @Builder.Default
    private String locale = "en-US";

    @Schema(description = "Set to false to disable this template")
    @Builder.Default
    private boolean active = true;
}
