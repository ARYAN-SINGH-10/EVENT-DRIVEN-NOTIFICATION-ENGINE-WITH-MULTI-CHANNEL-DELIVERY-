package com.notificationengine.config;

import com.notificationengine.dto.DlqStatsResponse;
import com.notificationengine.provider.DeliveryProvider;
import com.notificationengine.provider.DeliveryProviderService;
import com.notificationengine.service.DlqService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Spring Boot Actuator Health Indicator for the Notification Engine.
 * Exposes real-time provider health, DLQ backlog status, and execution environment mode.
 */
@Component
@RequiredArgsConstructor
public class NotificationEngineHealthIndicator implements HealthIndicator {

    private final DeliveryProviderService providerService;
    private final DlqService dlqService;

    @Value("${app.kafka.simulation-mode:true}")
    private boolean simulationMode;

    @Override
    public Health health() {
        Collection<DeliveryProvider> providers = providerService.getAllProviders();
        long healthyCount = providers.stream().filter(DeliveryProvider::isHealthy).count();
        long totalProviders = providers.size();

        DlqStatsResponse dlqStats = dlqService.getStats();
        long pendingDlq = dlqStats != null && dlqStats.getCountsByStatus() != null
                ? dlqStats.getCountsByStatus().getOrDefault(com.notificationengine.entity.DeadLetterQueueEntry.DlqStatus.PENDING_RETRY, 0L)
                : 0L;

        Map<String, Object> details = new HashMap<>();
        details.put("environmentMode", simulationMode ? "KAFKA_SIMULATION" : "REAL_KAFKA_CONNECTED");
        details.put("providersTotal", totalProviders);
        details.put("providersHealthy", healthyCount);
        details.put("dlqBacklogPending", pendingDlq);

        Map<String, String> providerStatus = new HashMap<>();
        for (DeliveryProvider p : providers) {
            providerStatus.put(p.getChannel().name(), p.isHealthy() ? "UP" : "DOWN");
        }
        details.put("channels", providerStatus);

        if (totalProviders > 0 && healthyCount == 0) {
            return Health.down().withDetails(details).build();
        }

        return Health.up().withDetails(details).build();
    }
}
