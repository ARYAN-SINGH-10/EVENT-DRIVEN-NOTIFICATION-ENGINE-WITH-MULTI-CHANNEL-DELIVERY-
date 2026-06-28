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
 * Simulated In-App Delivery Provider.
 */
@Slf4j
@Component
public class InAppProvider implements DeliveryProvider {

    private final Map<String, NotificationStatus> statusStore = new ConcurrentHashMap<>();

    @Value("${app.channel.inapp.simulation.healthy:true}")
    private boolean isHealthy = true;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public String getName() {
        return "SimulatedInAppProvider (Websockets Inbox)";
    }

    @Override
    public String send(String recipient, String subject, String body) {
        if (!isHealthy()) {
            throw new IllegalStateException("In-app provider is currently unhealthy/unavailable.");
        }
        if (!validateRecipient(recipient)) {
            throw new IllegalArgumentException("Invalid user ID / recipient format for in-app alert: " + recipient);
        }

        String externalId = "ext-inapp-" + System.currentTimeMillis() + "-" + Math.abs(recipient.hashCode() % 1000);
        log.info("[IN-APP PROVIDER] Dispatching in-app notification to user={}, subject={}, externalId={}", recipient, subject, externalId);
        
        // Simulating immediate delivery status
        statusStore.put(externalId, NotificationStatus.DELIVERED);
        return externalId;
    }

    @Override
    public boolean validateRecipient(String recipient) {
        return recipient != null && !recipient.isBlank();
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
