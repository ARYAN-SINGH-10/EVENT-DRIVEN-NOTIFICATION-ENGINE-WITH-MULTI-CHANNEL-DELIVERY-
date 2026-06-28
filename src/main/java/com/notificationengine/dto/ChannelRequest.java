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
 * Request DTO for creating or updating a notification channel configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Channel configuration request")
public class ChannelRequest {

    @NotNull(message = "channelType is required")
    @Schema(description = "Channel type", requiredMode = Schema.RequiredMode.REQUIRED)
    private ChannelType channelType;

    @NotBlank(message = "displayName is required")
    @Size(max = 100)
    @Schema(description = "Human-readable channel name", example = "Email Channel")
    private String displayName;

    @Size(max = 500)
    @Schema(description = "Channel description")
    private String description;

    @Schema(description = "Whether this channel is active")
    @Builder.Default
    private boolean active = true;

    @Schema(description = "Whether to simulate delivery (no real messages sent)")
    @Builder.Default
    private boolean simulationMode = true;

    @Schema(description = "JSON-encoded provider configuration")
    private String providerConfig;

    @Min(1) @Max(100000)
    @Schema(description = "Rate limit in messages per minute", example = "1000")
    @Builder.Default
    private int rateLimitPerMinute = 1000;

    @Schema(description = "Whether the channel supports HTML content")
    private boolean supportsHtml;

    @Schema(description = "Whether the channel supports attachments")
    private boolean supportsAttachments;
}
