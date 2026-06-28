package com.notificationengine.service;

import com.notificationengine.dto.ComplianceRequest;
import com.notificationengine.dto.ComplianceResponse;
import com.notificationengine.entity.ComplianceRecord.ConsentType;
import org.springframework.data.domain.Pageable;
import com.notificationengine.dto.PagedResponse;

import java.util.List;
import java.util.UUID;

public interface ComplianceService {
    ComplianceResponse recordConsent(ComplianceRequest request);
    List<ComplianceResponse> getByUserId(String userId);
    ComplianceResponse getById(UUID id);
    void revokeAllConsents(String userId);
    boolean hasConsent(String userId, ConsentType consentType);
    PagedResponse<ComplianceResponse> getPendingDeletions(Pageable pageable);
}
