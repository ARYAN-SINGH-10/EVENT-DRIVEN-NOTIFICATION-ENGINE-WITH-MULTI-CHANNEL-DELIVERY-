package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.UserPreference.NotificationFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating user notification preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User notification preference request")
public class UserPreferenceRequest {

    @NotBlank(message = "userId is required")
    @Size(max = 100)
    @Schema(description = "User identifier", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @NotNull(message = "channelType is required")
    @Schema(description = "Notification channel", requiredMode = Schema.RequiredMode.REQUIRED)
    private ChannelType channelType;

    @Schema(description = "Enable or disable this channel for the user")
    @Builder.Default
    private boolean enabled = true;

    @Schema(description = "Comma-separated event types to opt out of")
    private String optedOutEventTypes;

    @Schema(description = "Notification delivery frequency")
    @Builder.Default
    private NotificationFrequency frequency = NotificationFrequency.IMMEDIATE;

    @Pattern(regexp = "([01]?[0-9]|2[0-3]):[0-5][0-9]", message = "quietHoursStart must be HH:mm")
    @Schema(description = "Quiet hours start time (HH:mm)", example = "22:00")
    private String quietHoursStart;

    @Pattern(regexp = "([01]?[0-9]|2[0-3]):[0-5][0-9]", message = "quietHoursEnd must be HH:mm")
    @Schema(description = "Quiet hours end time (HH:mm)", example = "07:00")
    private String quietHoursEnd;

    @Size(max = 50)
    @Schema(description = "User timezone", example = "America/New_York")
    @Builder.Default
    private String timezone = "UTC";

    @Pattern(regexp = "[a-z]{2}-[A-Z]{2}", message = "locale must be in format en-US")
    @Schema(description = "User preferred locale", example = "en-US")
    @Builder.Default
    private String locale = "en-US";

    @Schema(description = "Priority override for this channel (e.g. CRITICAL, HIGH)")
    private String priorityOverride;
}
