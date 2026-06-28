package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.ComplianceRecord.ConsentStatus;
import com.notificationengine.entity.ComplianceRecord.ConsentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for a compliance record. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComplianceResponse {
    private UUID id;
    private String userId;
    private ConsentType consentType;
    private ConsentStatus consentStatus;
    private String channelType;
    private String consentVersion;
    private LocalDateTime consentGivenAt;
    private LocalDateTime consentRevokedAt;
    private LocalDateTime dataRetentionUntil;
    private LocalDateTime deletionRequestedAt;
    private LocalDateTime deletionCompletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
