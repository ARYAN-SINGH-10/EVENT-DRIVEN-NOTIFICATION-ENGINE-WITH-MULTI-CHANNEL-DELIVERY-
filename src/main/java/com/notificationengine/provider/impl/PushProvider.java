package com.notificationengine.provider.impl;

import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.provider.DeliveryProvider;
import com.notificationengine.provider.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulated Push Notification Provider.
 */
@Slf4j
@Component
public class PushProvider implements DeliveryProvider {

    private final Map<String, NotificationStatus> statusStore = new ConcurrentHashMap<>();

    @Value("${app.channel.push.simulation.healthy:true}")
    private boolean isHealthy = true;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public String getName() {
        return "SimulatedPushProvider (Firebase APNS)";
    }

    @Override
    public String send(String recipient, String subject, String body) {
        if (!isHealthy()) {
            throw new IllegalStateException("Push provider is currently unhealthy/unavailable.");
        }
        if (!validateRecipient(recipient)) {
            throw new IllegalArgumentException("Invalid device token format: " + recipient);
        }

        String externalId = "ext-push-" + System.currentTimeMillis() + "-" + Math.abs(recipient.hashCode() % 1000);
        log.info("[PUSH PROVIDER] Sending push alert to token={}, subject={}, externalId={}", recipient, subject, externalId);
        
        // Simulating push delivery
        statusStore.put(externalId, NotificationStatus.DELIVERED);
        return externalId;
    }

    @Override
    public boolean validateRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) return false;
        return recipient.trim().length() >= 10;
    }

    @Override
    public NotificationStatus getDeliveryStatus(String externalMessageId) {
        return statusStore.getOrDefault(externalMessageId, NotificationStatus.FAILED);
    }

    @Override
    public boolean isHealthy() {
        return isHealthy;
    }

    public void setHealthy(boolean healthy) {
        this.isHealthy = healthy;
    }
}
