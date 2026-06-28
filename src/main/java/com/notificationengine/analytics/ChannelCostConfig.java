package com.notificationengine.analytics;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-channel cost rates in USD per notification.
 * Values are configurable via application properties.
 */
@Component
public class ChannelCostConfig {

    @Value("${app.analytics.cost.sms:0.0075}")
    private double smsCostPerUnit;

    @Value("${app.analytics.cost.email:0.001}")
    private double emailCostPerUnit;

    @Value("${app.analytics.cost.push:0.0005}")
    private double pushCostPerUnit;

    @Value("${app.analytics.cost.whatsapp:0.005}")
    private double whatsappCostPerUnit;

    @Value("${app.analytics.cost.in_app:0.0}")
    private double inAppCostPerUnit;

    public double getCostPerUnit(ChannelType channelType) {
        return switch (channelType) {
            case SMS       -> smsCostPerUnit;
            case EMAIL     -> emailCostPerUnit;
            case PUSH      -> pushCostPerUnit;
            case WHATSAPP  -> whatsappCostPerUnit;
            case IN_APP    -> inAppCostPerUnit;
            case SLACK, WEBHOOK, CALL -> 0.0;  // infrastructure channels, no direct cost
        };
    }

    public Map<ChannelType, Double> getAllCosts() {
        Map<ChannelType, Double> map = new EnumMap<>(ChannelType.class);
        for (ChannelType ct : ChannelType.values()) {
            map.put(ct, getCostPerUnit(ct));
        }
        return map;
    }
}
