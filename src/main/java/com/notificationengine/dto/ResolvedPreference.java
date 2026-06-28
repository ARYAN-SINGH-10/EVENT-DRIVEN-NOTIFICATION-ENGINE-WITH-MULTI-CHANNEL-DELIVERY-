package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.UserPreference.NotificationFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the final calculated preference after evaluating the 4-tier hierarchy:
 * System Defaults -> Segment Overrides -> User Preferences -> Regulatory Overrides.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resolved preference after applying hierarchy rules")
public class ResolvedPreference {

    @Schema(description = "Whether the channel is ultimately enabled")
    private boolean enabled;

    @Schema(description = "Resolved delivery frequency")
    private NotificationFrequency frequency;

    @Schema(description = "Resolved quiet hours start (HH:mm)")
    private String quietHoursStart;

    @Schema(description = "Resolved quiet hours end (HH:mm)")
    private String quietHoursEnd;

    @Schema(description = "Resolved effective priority override")
    private String effectivePriority;

    @Schema(description = "Audit trail explaining why this preference was applied")
    private String resolutionReason;
}
