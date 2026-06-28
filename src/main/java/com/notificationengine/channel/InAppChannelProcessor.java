package com.notificationengine.channel;

import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.provider.DeliveryProviderService;
import com.notificationengine.provider.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-app channel processor delegating to the delivery provider layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InAppChannelProcessor implements NotificationChannelProcessor {

    private final DeliveryProviderService providerService;

    @Override
    public NotificationEntity.ChannelType getChannelType() {
        return NotificationEntity.ChannelType.IN_APP;
    }

    @Override
    public String send(NotificationEntity notification) {
        log.info("[IN-APP-PROCESSOR] Routing In-app request through provider abstraction layer");
        return providerService.send(
                NotificationChannel.IN_APP,
                notification.getRecipientAddress(),
                notification.getSubject(),
                notification.getBody()
        );
    }

    @Override
    public boolean isAvailable() {
        return providerService.isChannelHealthy(NotificationChannel.IN_APP);
    }
}
