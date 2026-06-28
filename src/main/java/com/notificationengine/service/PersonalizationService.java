package com.notificationengine.service;

import com.notificationengine.dto.PersonalizedPayload;

import java.util.Map;

/**
 * Orchestrates the Personalization Pipeline for notification enrichment.
 *
 * <p>Given a userId, event type, and raw variable map, this service:</p>
 * <ol>
 *   <li>Resolves the user's full profile (locale, timezone, currency, risk profile)</li>
 *   <li>Formats monetary and date values to the user's locale</li>
 *   <li>Derives financial calculations (P&amp;L, portfolio impact, risk exposure)</li>
 *   <li>Returns a {@link PersonalizedPayload} ready to drive template rendering</li>
 * </ol>
 */
public interface PersonalizationService {

    /**
     * Runs the complete personalization pipeline.
     *
     * @param userId       target user ID
     * @param eventType    financial event code (e.g. "TXNX-001")
     * @param rawVariables raw key-value pairs from the triggering event
     * @return enriched payload with all personalization fields merged into {@code variables}
     */
    PersonalizedPayload personalize(String userId, String eventType, Map<String, String> rawVariables);
}
