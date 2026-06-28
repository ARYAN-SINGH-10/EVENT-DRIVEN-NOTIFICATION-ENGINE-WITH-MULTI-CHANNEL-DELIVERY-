package com.notificationengine.service.impl;

import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.repository.NotificationRepository;
import com.notificationengine.service.FrequencyCapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrequencyCapServiceImpl implements FrequencyCapService {

    private final NotificationRepository notificationRepository;

    private static final int GLOBAL_DAILY_CAP = 12;
    private static final int SMS_DAILY_CAP = 5;
    private static final int PUSH_DAILY_CAP = 8;
    private static final int EMAIL_DAILY_CAP = 3;
    private static final int CATEGORY_DAILY_CAP = 3;
    private static final long COOLDOWN_MINUTES = 60;

    @Override
    @Transactional(readOnly = true)
    public boolean isWithinCaps(String userId, ChannelType channel, String eventType, String priority) {
        log.debug("[FREQ-CAP] Evaluating rules for user={} channel={} eventType={} priority={}", userId, channel, eventType, priority);

        // 1. Bypass Rule: CRITICAL priority bypasses all limits
        if ("CRITICAL".equalsIgnoreCase(priority)) {
            log.debug("[FREQ-CAP] Priority is CRITICAL. Bypassing frequency caps.");
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.with(LocalTime.MIN);
        LocalDateTime endOfDay = now.with(LocalTime.MAX);

        // 2. Cooldown Period (1 hour)
        Optional<NotificationEntity> lastNotification = notificationRepository
                .findTopByUserIdAndEventTypeOrderByCreatedAtDesc(userId, eventType);
        
        if (lastNotification.isPresent()) {
            LocalDateTime lastSentAt = lastNotification.get().getCreatedAt();
            long minutesSinceLast = ChronoUnit.MINUTES.between(lastSentAt, now);
            if (minutesSinceLast < COOLDOWN_MINUTES) {
                log.warn("[FREQ-CAP] Blocked by COOLDOWN rule. Last message sent {} mins ago (minimum {} mins required). user={}, eventType={}", 
                        minutesSinceLast, COOLDOWN_MINUTES, userId, eventType);
                return false;
            }
        }

        // 3. Global Daily Cap (Max 12)
        long totalToday = notificationRepository.countByUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay);
        if (totalToday >= GLOBAL_DAILY_CAP) {
            log.warn("[FREQ-CAP] Blocked by GLOBAL DAILY CAP. Total today: {} (Max {}). user={}", 
                    totalToday, GLOBAL_DAILY_CAP, userId);
            return false;
        }

        // 4. Per Channel Cap
        int channelCap = getChannelCap(channel);
        if (channelCap > 0) {
            long channelCount = notificationRepository.countByUserIdAndChannelTypeAndCreatedAtBetween(userId, channel, startOfDay, endOfDay);
            if (channelCount >= channelCap) {
                log.warn("[FREQ-CAP] Blocked by PER CHANNEL CAP. Channel {} count today: {} (Max {}). user={}", 
                        channel, channelCount, channelCap, userId);
                return false;
            }
        }

        // 5. Per Category Cap
        long categoryCount = notificationRepository.countByUserIdAndEventTypeAndCreatedAtBetween(userId, eventType, startOfDay, endOfDay);
        if (categoryCount >= CATEGORY_DAILY_CAP) {
            log.warn("[FREQ-CAP] Blocked by PER CATEGORY CAP. EventType {} count today: {} (Max {}). user={}", 
                    eventType, categoryCount, CATEGORY_DAILY_CAP, userId);
            return false;
        }

        log.debug("[FREQ-CAP] All checks passed for user={} channel={}", userId, channel);
        return true;
    }

    private int getChannelCap(ChannelType channel) {
        return switch (channel) {
            case SMS -> SMS_DAILY_CAP;
            case PUSH -> PUSH_DAILY_CAP;
            case EMAIL -> EMAIL_DAILY_CAP;
            default -> -1; // No explicit cap for other channels
        };
    }
}
