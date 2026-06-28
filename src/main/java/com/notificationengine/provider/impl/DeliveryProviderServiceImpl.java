package com.notificationengine.provider.impl;

import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.provider.DeliveryProvider;
import com.notificationengine.provider.DeliveryProviderService;
import com.notificationengine.provider.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of the DeliveryProviderService abstraction layer.
 * Dynamically resolves provider instances based on channel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryProviderServiceImpl implements DeliveryProviderService {

    private final List<DeliveryProvider> providers;

    @Override
    public DeliveryProvider getProvider(NotificationChannel channel) {
        return providers.stream()
                .filter(p -> p.getChannel() == channel)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider registered for channel: " + channel));
    }

    @Override
    public String send(NotificationChannel channel, String recipient, String subject, String body) {
        log.info("[PROVIDER-SERVICE] Directing send request to {} channel", channel);
        DeliveryProvider provider = getProvider(channel);
        return provider.send(recipient, subject, body);
    }

    @Override
    public boolean validateRecipient(NotificationChannel channel, String recipient) {
        try {
            DeliveryProvider provider = getProvider(channel);
            return provider.validateRecipient(recipient);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public NotificationStatus getDeliveryStatus(NotificationChannel channel, String externalMessageId) {
        DeliveryProvider provider = getProvider(channel);
        return provider.getDeliveryStatus(externalMessageId);
    }

    @Override
    public boolean isChannelHealthy(NotificationChannel channel) {
        try {
            DeliveryProvider provider = getProvider(channel);
            return provider.isHealthy();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public List<DeliveryProvider> getAllProviders() {
        return providers;
    }
}
