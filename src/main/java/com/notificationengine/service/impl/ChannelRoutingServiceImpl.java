package com.notificationengine.service.impl;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.dto.ResolvedPreference;
import com.notificationengine.provider.DeliveryProviderService;
import com.notificationengine.provider.NotificationChannel;
import com.notificationengine.service.ChannelRoutingService;
import com.notificationengine.service.PreferenceResolutionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelRoutingServiceImpl implements ChannelRoutingService {
    private final DeliveryProviderService providerService;
    private final PreferenceResolutionService preferenceResolutionService;

    // Threshold score for a channel to be selected
    private static final double SELECTION_THRESHOLD = 55.0;

    @Override
    @Transactional(readOnly = true)
    public List<ChannelType> determineOptimalChannels(String userId, String eventType, String priority) {
        log.info("[ROUTING] Routing event: type={}, user={}, priority={}", eventType, userId, priority);

        // We will evaluate regulatory overrides during the scoring loop via PreferenceResolutionService.

        // 2. Score channels and filter
        Map<ChannelType, Double> scores = getRoutingScores(userId, eventType, priority);
        log.debug("[ROUTING] Computed scores: {}", scores);

        List<ChannelType> selectedChannels = scores.entrySet().stream()
                .filter(entry -> entry.getValue() >= SELECTION_THRESHOLD)
                .sorted(Map.Entry.<ChannelType, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Fallback: If no channel meets the threshold, pick the highest scoring available channel
        if (selectedChannels.isEmpty() && !scores.isEmpty()) {
            ChannelType fallbackChannel = scores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(ChannelType.EMAIL);
            log.info("[ROUTING] No channels met the threshold of {}. Falling back to highest scoring channel: {}", SELECTION_THRESHOLD, fallbackChannel);
            selectedChannels = Collections.singletonList(fallbackChannel);
        }

        log.info("[ROUTING] Optimal channels determined: {}", selectedChannels);
        return selectedChannels;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ChannelType, Double> getRoutingScores(String userId, String eventType, String priority) {
        Map<ChannelType, Double> scores = new EnumMap<>(ChannelType.class);

        // List of channels supported by our delivery providers
        List<ChannelType> candidateChannels = Arrays.asList(
                ChannelType.EMAIL,
                ChannelType.SMS,
                ChannelType.PUSH,
                ChannelType.WHATSAPP,
                ChannelType.IN_APP
        );

        String upperPriority = priority != null ? priority.toUpperCase() : "NORMAL";

        // Weights by priority
        double wDelivery;
        double wCost;
        switch (upperPriority) {
            case "CRITICAL" -> {
                wDelivery = 1.0;
                wCost = 0.0;
            }
            case "HIGH" -> {
                wDelivery = 0.7;
                wCost = 0.3;
            }
            case "LOW" -> {
                wDelivery = 0.2;
                wCost = 0.8;
            }
            default -> { // NORMAL
                wDelivery = 0.5;
                wCost = 0.5;
            }
        }

        for (ChannelType channel : candidateChannels) {
            // Check provider health
            NotificationChannel providerChannel = mapToProviderChannel(channel);
            if (!providerService.isChannelHealthy(providerChannel)) {
                log.debug("[ROUTING] Excluding channel {} because its provider is unhealthy", channel);
                continue;
            }

            // Evaluate 4-Tier Preference Hierarchy
            ResolvedPreference resolvedPref = preferenceResolutionService.resolvePreference(userId, eventType, channel);
            
            if (!resolvedPref.isEnabled()) {
                log.debug("[ROUTING] Excluding channel {} because it is disabled after preference resolution (Reason: {})", channel, resolvedPref.getResolutionReason());
                continue;
            }

            // Calculate Delivery Score using the effective priority (which might be overridden)
            double deliveryScore = calculateDeliveryScore(channel, resolvedPref.getEffectivePriority());

            // Calculate Cost Score
            double costScore = calculateCostScore(channel);

            // Calculate final composite score
            double finalScore = (wDelivery * deliveryScore) + (wCost * costScore);
            scores.put(channel, finalScore);
        }

        return scores;
    }

    private double calculateDeliveryScore(ChannelType channel, String priority) {
        // Base reliability success rates (as percentage points out of 100)
        double baseReliability = switch (channel) {
            case IN_APP -> 99.0;
            case SMS -> 98.0;
            case WHATSAPP -> 97.0;
            case EMAIL -> 95.0;
            case PUSH -> 90.0;
            default -> 80.0;
        };

        // Latency bonus for fast channels (SMS, Push, WhatsApp) when priority is HIGH or CRITICAL
        double latencyBonus = 0.0;
        boolean isHighPriority = "HIGH".equals(priority) || "CRITICAL".equals(priority);
        if (isHighPriority) {
            latencyBonus = switch (channel) {
                case SMS, PUSH, WHATSAPP -> 30.0;
                default -> 0.0;
            };
        }

        return Math.min(100.0, baseReliability + latencyBonus);
    }

    private double calculateCostScore(ChannelType channel) {
        // Costs relative to SMS (which has cost of 1.0)
        double cost = switch (channel) {
            case SMS -> 1.0;
            case WHATSAPP -> 0.8;
            case PUSH -> 0.2;
            case EMAIL -> 0.1;
            case IN_APP -> 0.05;
            default -> 0.5;
        };

        return (1.0 - cost) * 100.0;
    }


    private NotificationChannel mapToProviderChannel(ChannelType type) {
        return switch (type) {
            case EMAIL -> NotificationChannel.EMAIL;
            case SMS -> NotificationChannel.SMS;
            case PUSH -> NotificationChannel.PUSH;
            case WHATSAPP -> NotificationChannel.WHATSAPP;
            case IN_APP -> NotificationChannel.IN_APP;
            default -> throw new IllegalArgumentException("Unsupported provider channel mapping for: " + type);
        };
    }
}
