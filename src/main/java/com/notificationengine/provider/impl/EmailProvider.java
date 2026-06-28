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
 * Simulated Email Delivery Provider.
 */
@Slf4j
@Component
public class EmailProvider implements DeliveryProvider {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final Map<String, NotificationStatus> statusStore = new ConcurrentHashMap<>();

    @Value("${app.channel.email.simulation.healthy:true}")
    private boolean isHealthy = true;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public String getName() {
        return "SimulatedEmailProvider (SendGrid)";
    }

    @Override
    public String send(String recipient, String subject, String body) {
        if (!isHealthy()) {
            throw new IllegalStateException("Email provider is currently unhealthy/unavailable.");
        }
        if (!validateRecipient(recipient)) {
            throw new IllegalArgumentException("Invalid email format: " + recipient);
        }

        String externalId = "ext-email-" + System.currentTimeMillis() + "-" + Math.abs(recipient.hashCode() % 1000);
        log.info("[EMAIL PROVIDER] Sending email to={}, subject={}, externalId={}", recipient, subject, externalId);
        
        // Simulating immediate delivery
        statusStore.put(externalId, NotificationStatus.DELIVERED);
        return externalId;
    }

    @Override
    public boolean validateRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) return false;
        return EMAIL_PATTERN.matcher(recipient).matches();
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
