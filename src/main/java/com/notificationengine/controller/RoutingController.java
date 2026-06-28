package com.notificationengine.controller;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.service.ChannelRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/routing")
@RequiredArgsConstructor
@Tag(name = "Channel Routing API", description = "Inspect intelligent channel routing decisions and scores")
public class RoutingController {

    private final ChannelRoutingService routingService;

    @GetMapping
    @Operation(summary = "Get optimal delivery channels for a user/event/priority combination")
    public ResponseEntity<Map<String, Object>> getOptimalChannels(
            @RequestParam String userId,
            @RequestParam String eventType,
            @RequestParam(defaultValue = "NORMAL") String priority) {

        List<ChannelType> channels = routingService.determineOptimalChannels(userId, eventType, priority);
        Map<ChannelType, Double> scores = routingService.getRoutingScores(userId, eventType, priority);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("eventType", eventType);
        response.put("priority", priority);
        response.put("optimalChannels", channels);
        response.put("scores", scores);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/scores")
    @Operation(summary = "Get the full routing score breakdown for each channel")
    public ResponseEntity<Map<String, Object>> getScores(
            @RequestParam String userId,
            @RequestParam String eventType,
            @RequestParam(defaultValue = "NORMAL") String priority) {

        Map<ChannelType, Double> scores = routingService.getRoutingScores(userId, eventType, priority);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("eventType", eventType);
        response.put("priority", priority);
        response.put("scores", scores);
        return ResponseEntity.ok(response);
    }
}
