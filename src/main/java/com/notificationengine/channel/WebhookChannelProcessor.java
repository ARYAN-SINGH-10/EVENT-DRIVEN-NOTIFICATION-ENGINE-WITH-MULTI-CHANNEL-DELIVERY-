package com.notificationengine.channel;

import com.notificationengine.entity.NotificationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Webhook channel processor.
 *
 * <p>In simulation mode, logs the outbound webhook payload.
 * In real mode, POSTs the notification body to the recipient URL.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookChannelProcessor implements NotificationChannelProcessor {

    private final RestTemplate restTemplate;

    @Value("${app.channel.webhook.simulation:true}")
    private boolean simulationMode;

    @Override
    public NotificationEntity.ChannelType getChannelType() {
        return NotificationEntity.ChannelType.WEBHOOK;
    }

    @Override
    public String send(NotificationEntity notification) {
        String url = notification.getRecipientAddress();

        if (simulationMode) {
            log.info("[WEBHOOK SIMULATION] POST to={} payload='{}'", url, notification.getBody());
            return "sim-webhook-" + System.currentTimeMillis();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(notification.getBody(), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            log.info("[WEBHOOK] POST to={} status={}", url, response.getStatusCode());
            return "webhook-" + response.getStatusCode().value() + "-" + System.currentTimeMillis();
        } catch (Exception e) {
            log.error("[WEBHOOK] Failed to POST to={}: {}", url, e.getMessage());
            throw new com.notificationengine.exception.ChannelUnavailableException("WEBHOOK", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
