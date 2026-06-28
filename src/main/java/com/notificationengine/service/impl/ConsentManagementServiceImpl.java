package com.notificationengine.service.impl;

import com.notificationengine.compliance.ConsentManager;
import com.notificationengine.entity.ComplianceRecord.ConsentType;
import com.notificationengine.service.ConsentManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentManagementServiceImpl implements ConsentManagementService {

    private final ConsentManager consentManager;

    @Override
    @Transactional(readOnly = true)
    public boolean hasWhatsAppConsent(String userId) {
        return consentManager.hasConsent(userId, ConsentType.WHATSAPP_EXPLICIT);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPromotionalConsent(String userId) {
        return consentManager.hasConsent(userId, ConsentType.MARKETING);
    }
}
