package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.ComplianceRecord.ConsentStatus;
import com.notificationengine.entity.ComplianceRecord.ConsentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for recording user consent. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Compliance / consent request")
public class ComplianceRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotNull(message = "consentType is required")
    private ConsentType consentType;

    @NotNull(message = "consentStatus is required")
    private ConsentStatus consentStatus;

    private String channelType;
    private String ipAddress;
    private String userAgent;
    private String consentVersion;
    private String notes;
}
