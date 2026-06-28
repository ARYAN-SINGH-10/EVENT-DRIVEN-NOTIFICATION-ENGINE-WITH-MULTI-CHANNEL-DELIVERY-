package com.notificationengine.service;

import com.notificationengine.dto.ResolvedPreference;
import com.notificationengine.entity.NotificationEntity.ChannelType;

public interface PreferenceResolutionService {

    /**
     * Resolves the final effective user preference for a given event, user, and channel
     * by applying the 4-tier hierarchy (Defaults -> Segment -> User -> Regulatory).
     */
    ResolvedPreference resolvePreference(String userId, String eventType, ChannelType channel);
}
