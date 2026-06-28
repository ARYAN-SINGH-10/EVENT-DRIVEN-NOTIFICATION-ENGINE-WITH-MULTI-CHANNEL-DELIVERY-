package com.notificationengine.provider.impl;

import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.provider.DeliveryProvider;
import com.notificationengine.provider.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Simulated WhatsApp Delivery Provider.
 */
@Slf4j
@Component
public class WhatsAppProvider implements DeliveryProvider {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^whatsapp:\\+?[1-9]\\d{6,14}$");

    private static final Pattern RAW_PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{6,14}$");

    private final Map<String, NotificationStatus> statusStore = new ConcurrentHashMap<>();

    @Value("${app.channel.whatsapp.simulation.healthy:true}")
    private boolean isHealthy = true;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public String getName() {
        return "SimulatedWhatsAppProvider (Meta Cloud API)";
    }

    @Override
    public String send(String recipient, String subject, String body) {
        if (!isHealthy()) {
            throw new IllegalStateException("WhatsApp provider is currently unhealthy/unavailable.");
        }
        if (!validateRecipient(recipient)) {
            throw new IllegalArgumentException("Invalid WhatsApp recipient format: " + recipient);
        }

        String externalId = "ext-wa-" + System.currentTimeMillis() + "-" + Math.abs(recipient.hashCode() % 1000);
        log.info("[WHATSAPP PROVIDER] Sending WhatsApp alert to={}, externalId={}", recipient, externalId);
        
        // Simulating WhatsApp delivery status
        statusStore.put(externalId, NotificationStatus.DELIVERED);
        return externalId;
    }

    @Override
    public boolean validateRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) return false;
        return PHONE_PATTERN.matcher(recipient).matches() || RAW_PHONE_PATTERN.matcher(recipient).matches();
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
