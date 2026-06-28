package com.notificationengine.service.impl;

import com.notificationengine.dto.ResolvedPreference;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.UserPreference;
import com.notificationengine.entity.UserPreference.NotificationFrequency;
import com.notificationengine.entity.UserProfile;
import com.notificationengine.personalization.UserProfileResolver;
import com.notificationengine.repository.UserPreferenceRepository;
import com.notificationengine.service.PreferenceResolutionService;
import com.notificationengine.taxonomy.FinancialEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceResolutionServiceImpl implements PreferenceResolutionService {

    private final UserProfileResolver userProfileResolver;
    private final UserPreferenceRepository userPreferenceRepository;

    @Override
    public ResolvedPreference resolvePreference(String userId, String eventType, ChannelType channel) {
        log.debug("[PREF-RESOLUTION] Resolving preference for userId={}, eventType={}, channel={}", userId, eventType, channel);

        // ---------------------------------------------------------------------
        // Tier 1: System Defaults
        // ---------------------------------------------------------------------
        boolean enabled = true;
        NotificationFrequency frequency = NotificationFrequency.IMMEDIATE;
        String quietHoursStart = null;
        String quietHoursEnd = null;
        String effectivePriority = "NORMAL";
        String reason = "Tier 1: System Default";

        // ---------------------------------------------------------------------
        // Tier 2: Segment Overrides (Based on UserProfile)
        // ---------------------------------------------------------------------
        UserProfile profile = userProfileResolver.resolve(userId);
        
        // Example segment override: Conservative risk profile gets automatic quiet hours
        if (profile.getRiskProfile() == UserProfile.RiskProfile.CONSERVATIVE) {
            quietHoursStart = "22:00";
            quietHoursEnd = "08:00";
            reason = "Tier 2: Segment Override (Conservative Profile automatic quiet hours)";
        }
        
        // Example segment override: Premium users default to having SMS enabled, others default SMS to false (unless explicit DB pref)
        if (channel == ChannelType.SMS && !profile.isPremiumAccount()) {
            enabled = false;
            reason = "Tier 2: Segment Override (SMS default disabled for non-premium)";
        } else if (channel == ChannelType.SMS && profile.isPremiumAccount()) {
            enabled = true;
            reason = "Tier 2: Segment Override (SMS default enabled for premium)";
        }

        // ---------------------------------------------------------------------
        // Tier 3: User Preferences (Database)
        // ---------------------------------------------------------------------
        Optional<UserPreference> prefOpt = userPreferenceRepository.findByUserIdAndChannelType(userId, channel);
        if (prefOpt.isPresent()) {
            UserPreference pref = prefOpt.get();

            // Opt-out logic (channel level)
            if (!pref.isEnabled()) {
                enabled = false;
                reason = "Tier 3: User Preference (Channel explicitly disabled)";
            } else {
                // Event-level opt-out
                if (pref.getOptedOutEventTypes() != null && !pref.getOptedOutEventTypes().isBlank()) {
                    List<String> optOutList = Arrays.stream(pref.getOptedOutEventTypes().split(","))
                            .map(String::trim).map(String::toUpperCase).toList();
                    if (optOutList.contains(eventType.toUpperCase())) {
                        enabled = false;
                        reason = "Tier 3: User Preference (Opted out of event type: " + eventType + ")";
                    } else {
                        enabled = true; // explicitly enabled
                        reason = "Tier 3: User Preference (Channel enabled)";
                    }
                } else {
                    enabled = true;
                    reason = "Tier 3: User Preference (Channel enabled)";
                }
            }

            frequency = pref.getFrequency();
            if (pref.getQuietHoursStart() != null) quietHoursStart = pref.getQuietHoursStart();
            if (pref.getQuietHoursEnd() != null) quietHoursEnd = pref.getQuietHoursEnd();
            
            if (pref.getPriorityOverride() != null && !pref.getPriorityOverride().isBlank()) {
                effectivePriority = pref.getPriorityOverride().toUpperCase();
                reason += " | Priority Overridden to " + effectivePriority;
            }
        }

        // ---------------------------------------------------------------------
        // Tier 4: Regulatory Overrides (Taxonomy)
        // Highest priority - overrides user opt-outs and quiet hours
        // ---------------------------------------------------------------------
        Optional<FinancialEventType> taxonomyOpt = Arrays.stream(FinancialEventType.values())
                .filter(e -> e.getEventCode().equalsIgnoreCase(eventType) || e.name().equalsIgnoreCase(eventType))
                .findFirst();

        if (taxonomyOpt.isPresent() && taxonomyOpt.get().isRegulatoryFlag()) {
            FinancialEventType taxonomy = taxonomyOpt.get();
            // If the channel is part of the regulatory default channels, force enable it
            if (taxonomy.getDefaultChannels().contains(channel)) {
                enabled = true;
                frequency = NotificationFrequency.IMMEDIATE;
                quietHoursStart = null;
                quietHoursEnd = null;
                effectivePriority = taxonomy.getPriority(); // Force regulatory priority
                reason = "Tier 4: Regulatory Override (Mandatory delivery)";
            }
        }

        log.debug("[PREF-RESOLUTION] Resolved preference: enabled={}, reason='{}'", enabled, reason);

        return ResolvedPreference.builder()
                .enabled(enabled)
                .frequency(frequency)
                .quietHoursStart(quietHoursStart)
                .quietHoursEnd(quietHoursEnd)
                .effectivePriority(effectivePriority)
                .resolutionReason(reason)
                .build();
    }
}
