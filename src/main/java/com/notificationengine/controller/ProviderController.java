package com.notificationengine.controller;

import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.provider.DeliveryProvider;
import com.notificationengine.provider.DeliveryProviderService;
import com.notificationengine.provider.NotificationChannel;
import com.notificationengine.provider.impl.EmailProvider;
import com.notificationengine.provider.impl.SmsProvider;
import com.notificationengine.provider.impl.PushProvider;
import com.notificationengine.provider.impl.WhatsAppProvider;
import com.notificationengine.provider.impl.InAppProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Tag(name = "Provider Abstraction API", description = "Endpoints for managing and querying delivery providers")
public class ProviderController {

    private final DeliveryProviderService providerService;

    @GetMapping("/health")
    @Operation(summary = "Get health status of all registered providers")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> healthMap = new HashMap<>();
        for (DeliveryProvider provider : providerService.getAllProviders()) {
            Map<String, Object> details = new HashMap<>();
            details.put("channel", provider.getChannel());
            details.put("name", provider.getName());
            details.put("healthy", provider.isHealthy());
            healthMap.put(provider.getChannel().name(), details);
        }
        return ResponseEntity.ok(healthMap);
    }

    @PostMapping("/{channel}/health")
    @Operation(summary = "Manually trigger/simulate provider health status (for testing)")
    public ResponseEntity<Map<String, Object>> setHealth(
            @PathVariable NotificationChannel channel,
            @RequestParam boolean healthy) {
        
        DeliveryProvider provider = providerService.getProvider(channel);
        if (provider instanceof EmailProvider) {
            ((EmailProvider) provider).setHealthy(healthy);
        } else if (provider instanceof SmsProvider) {
            ((SmsProvider) provider).setHealthy(healthy);
        } else if (provider instanceof PushProvider) {
            ((PushProvider) provider).setHealthy(healthy);
        } else if (provider instanceof WhatsAppProvider) {
            ((WhatsAppProvider) provider).setHealthy(healthy);
        } else if (provider instanceof InAppProvider) {
            ((InAppProvider) provider).setHealthy(healthy);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("channel", channel);
        response.put("name", provider.getName());
        response.put("healthy", provider.isHealthy());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{channel}/validate")
    @Operation(summary = "Validate recipient address format for a channel")
    public ResponseEntity<Map<String, Object>> validateRecipient(
            @PathVariable NotificationChannel channel,
            @RequestParam String recipient) {
        
        boolean isValid = providerService.validateRecipient(channel, recipient);
        Map<String, Object> response = new HashMap<>();
        response.put("channel", channel);
        response.put("recipient", recipient);
        response.put("valid", isValid);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{channel}/status/{externalId}")
    @Operation(summary = "Query delivery status from the provider using external message ID")
    public ResponseEntity<Map<String, Object>> getDeliveryStatus(
            @PathVariable NotificationChannel channel,
            @PathVariable String externalId) {
        
        NotificationStatus status = providerService.getDeliveryStatus(channel, externalId);
        Map<String, Object> response = new HashMap<>();
        response.put("channel", channel);
        response.put("externalMessageId", externalId);
        response.put("status", status);
        return ResponseEntity.ok(response);
    }
}
