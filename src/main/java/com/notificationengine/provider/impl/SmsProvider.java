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
 * Simulated SMS Delivery Provider.
 */
@Slf4j
@Component
public class SmsProvider implements DeliveryProvider {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{6,14}$");

    private final Map<String, NotificationStatus> statusStore = new ConcurrentHashMap<>();

    @Value("${app.channel.sms.simulation.healthy:true}")
    private boolean isHealthy = true;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public String getName() {
        return "SimulatedSmsProvider (Twilio)";
    }

    @Override
    public String send(String recipient, String subject, String body) {
        if (!isHealthy()) {
            throw new IllegalStateException("SMS provider is currently unhealthy/unavailable.");
        }
        if (!validateRecipient(recipient)) {
            throw new IllegalArgumentException("Invalid phone number format: " + recipient);
        }

        String externalId = "ext-sms-" + System.currentTimeMillis() + "-" + Math.abs(recipient.hashCode() % 1000);
        log.info("[SMS PROVIDER] Sending SMS to={}, externalId={}", recipient, externalId);
        
        // Simulating delivery success
        statusStore.put(externalId, NotificationStatus.DELIVERED);
        return externalId;
    }

    @Override
    public boolean validateRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) return false;
        return PHONE_PATTERN.matcher(recipient).matches();
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
