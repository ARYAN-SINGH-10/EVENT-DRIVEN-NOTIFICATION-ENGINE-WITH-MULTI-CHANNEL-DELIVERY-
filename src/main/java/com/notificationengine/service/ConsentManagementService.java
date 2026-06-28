package com.notificationengine.service;

public interface ConsentManagementService {

    /**
     * Checks if a user has explicitly opted in to receive WhatsApp communications.
     */
    boolean hasWhatsAppConsent(String userId);

    /**
     * Checks if a user has granted general marketing/promotional consent.
     */
    boolean hasPromotionalConsent(String userId);
}
