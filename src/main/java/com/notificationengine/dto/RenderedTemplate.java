package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Result of a successful template render operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of rendering a notification template")
public class RenderedTemplate {

    @Schema(description = "ID of the resolved template")
    private UUID templateId;

    @Schema(description = "Name of the resolved template")
    private String templateName;

    @Schema(description = "Version of the template used")
    private int version;

    @Schema(description = "Locale used for rendering")
    private String locale;

    @Schema(description = "Whether the locale was an exact match or a fallback")
    private boolean localeFallback;

    @Schema(description = "Channel type")
    private ChannelType channelType;

    @Schema(description = "Rendered subject line")
    private String subject;

    @Schema(description = "Rendered plain-text body")
    private String body;

    @Schema(description = "Rendered HTML body (for email channel)")
    private String htmlBody;

    @Schema(description = "Variables that were substituted during rendering")
    private Set<String> resolvedVariables;

    @Schema(description = "Variables present in the template that had no value supplied (left empty)")
    private Set<String> missingVariables;
}
