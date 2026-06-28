package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.UserPreference.NotificationFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user notification preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User notification preference response")
public class UserPreferenceResponse {

    private UUID id;
    private String userId;
    private ChannelType channelType;
    private boolean enabled;
    private String optedOutEventTypes;
    private NotificationFrequency frequency;
    private String quietHoursStart;
    private String quietHoursEnd;
    private String timezone;
    private String locale;
    private String priorityOverride;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
