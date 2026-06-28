package com.notificationengine.service.impl;

import com.notificationengine.compliance.ConsentManager;
import com.notificationengine.dto.ComplianceRequest;
import com.notificationengine.dto.ComplianceResponse;
import com.notificationengine.dto.PagedResponse;
import com.notificationengine.entity.ComplianceRecord;
import com.notificationengine.entity.ComplianceRecord.ConsentStatus;
import com.notificationengine.entity.ComplianceRecord.ConsentType;
import com.notificationengine.exception.ErrorCode;
import com.notificationengine.exception.NotificationException;
import com.notificationengine.repository.ComplianceRecordRepository;
import com.notificationengine.service.ComplianceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceServiceImpl implements ComplianceService {

    private final ComplianceRecordRepository complianceRecordRepository;
    private final ConsentManager consentManager;

    @Override
    @Transactional
    public ComplianceResponse recordConsent(ComplianceRequest request) {
        ComplianceRecord record = consentManager.recordConsent(
                request.getUserId(), request.getConsentType(), request.getConsentStatus(),
                request.getIpAddress(), request.getUserAgent(), request.getConsentVersion());
        return toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceResponse> getByUserId(String userId) {
        return complianceRecordRepository.findByUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceResponse getById(UUID id) {
        return complianceRecordRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Compliance record not found: " + id, 404));
    }

    @Override
    @Transactional
    public void revokeAllConsents(String userId) {
        consentManager.revokeAllConsents(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConsent(String userId, ConsentType consentType) {
        return consentManager.hasConsent(userId, consentType);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ComplianceResponse> getPendingDeletions(Pageable pageable) {
        var page = complianceRecordRepository.findByConsentStatus(ConsentStatus.DELETION_REQUESTED, pageable)
                .map(this::toResponse);
        return PagedResponse.from(page);
    }

    private ComplianceResponse toResponse(ComplianceRecord r) {
        return ComplianceResponse.builder()
                .id(r.getId()).userId(r.getUserId()).consentType(r.getConsentType())
                .consentStatus(r.getConsentStatus()).channelType(r.getChannelType())
                .consentVersion(r.getConsentVersion()).consentGivenAt(r.getConsentGivenAt())
                .consentRevokedAt(r.getConsentRevokedAt()).dataRetentionUntil(r.getDataRetentionUntil())
                .deletionRequestedAt(r.getDeletionRequestedAt())
                .deletionCompletedAt(r.getDeletionCompletedAt())
                .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
                .build();
    }
}
