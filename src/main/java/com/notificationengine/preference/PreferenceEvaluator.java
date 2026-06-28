package com.notificationengine.preference;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.UserPreference;
import com.notificationengine.repository.UserPreferenceRepository;
import com.notificationengine.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Evaluates whether a notification should be delivered to a user
 * based on their channel preferences, opt-outs, and quiet hours.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreferenceEvaluator {

    private final UserPreferenceRepository preferenceRepository;

    /**
     * Returns true if the notification is allowed for the given user and channel.
     *
     * @param userId     target user ID
     * @param channel    target delivery channel
     * @param eventType  event type triggering the notification
     * @return true if delivery is permitted
     */
    public boolean isDeliveryAllowed(String userId, ChannelType channel, String eventType) {
        Optional<UserPreference> prefOpt = preferenceRepository.findByUserIdAndChannelType(userId, channel);

        if (prefOpt.isEmpty()) {
            // No preference configured — allow by default
            log.debug("No preference for userId={} channel={} — allowing delivery", userId, channel);
            return true;
        }

        UserPreference pref = prefOpt.get();

        // 1. Channel disabled
        if (!pref.isEnabled()) {
            log.debug("Channel {} disabled for userId={}", channel, userId);
            return false;
        }

        // 2. Event type opted out
        if (isEventTypeOptedOut(pref, eventType)) {
            log.debug("userId={} opted out of eventType={} on channel={}", userId, eventType, channel);
            return false;
        }

        // 3. Quiet hours check
        if (isWithinQuietHours(pref)) {
            log.debug("userId={} is in quiet hours on channel={}", userId, channel);
            return false;
        }

        return true;
    }

    private boolean isEventTypeOptedOut(UserPreference pref, String eventType) {
        if (StringUtils.isBlank(pref.getOptedOutEventTypes()) || StringUtils.isBlank(eventType)) return false;
        String[] optedOut = pref.getOptedOutEventTypes().split(",");
        for (String optedOutType : optedOut) {
            if (optedOutType.trim().equalsIgnoreCase(eventType)) return true;
        }
        return false;
    }

    private boolean isWithinQuietHours(UserPreference pref) {
        if (StringUtils.isBlank(pref.getQuietHoursStart()) || StringUtils.isBlank(pref.getQuietHoursEnd())) {
            return false;
        }
        try {
            ZoneId userZone = ZoneId.of(pref.getTimezone() != null ? pref.getTimezone() : "UTC");
            LocalTime userLocalTime = ZonedDateTime.now(userZone).toLocalTime();
            return DateTimeUtil.isWithinQuietHours(userLocalTime, pref.getQuietHoursStart(), pref.getQuietHoursEnd());
        } catch (Exception e) {
            log.warn("Failed to evaluate quiet hours for userId={}: {}", pref.getUserId(), e.getMessage());
            return false;
        }
    }

    /**
     * Returns all channels where delivery is currently allowed.
     */
    public List<ChannelType> getAllowedChannels(String userId, String eventType) {
        return preferenceRepository.findEnabledPreferencesForUser(userId)
                .stream()
                .filter(pref -> !isEventTypeOptedOut(pref, eventType) && !isWithinQuietHours(pref))
                .map(UserPreference::getChannelType)
                .toList();
    }
}
