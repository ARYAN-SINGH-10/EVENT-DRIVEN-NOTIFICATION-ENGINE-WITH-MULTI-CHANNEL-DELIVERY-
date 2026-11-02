package com.notificationengine.service.impl;

import com.notificationengine.dto.EventPayload;
import com.notificationengine.dto.NotificationRequest;
import com.notificationengine.dto.NotificationResponse;
import com.notificationengine.dto.PersonalizedPayload;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEvent;
import com.notificationengine.entity.NotificationEvent.EventPriority;
import com.notificationengine.event.DomainNotificationEvent;
import com.notificationengine.event.KafkaNotificationProducer;
import com.notificationengine.event.SimulatedEventBus;
import com.notificationengine.repository.NotificationEventRepository;
import com.notificationengine.service.NotificationEventService;
import com.notificationengine.service.ChannelRoutingService;
import com.notificationengine.service.NotificationService;
import com.notificationengine.service.PersonalizationService;
import com.notificationengine.service.DndComplianceEngine;
import com.notificationengine.service.FrequencyCapService;
import com.notificationengine.service.QuietHoursService;
import com.notificationengine.service.QueuedNotificationService;
import com.notificationengine.service.PreferenceResolutionService;
import com.notificationengine.dto.ResolvedPreference;
import com.notificationengine.taxonomy.FinancialEventType;
import com.notificationengine.util.JsonUtil;
import com.notificationengine.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventServiceImpl implements NotificationEventService {

    private final NotificationEventRepository eventRepository;
    private final SimulatedEventBus simulatedEventBus;
    private final Optional<KafkaNotificationProducer> kafkaProducer;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationService notificationService;
    private final ChannelRoutingService channelRoutingService;
    private final PersonalizationService personalizationService;
    private final DndComplianceEngine dndComplianceEngine;
    private final FrequencyCapService frequencyCapService;
    private final QuietHoursService quietHoursService;
    private final QueuedNotificationService queuedNotificationService;
    private final PreferenceResolutionService preferenceResolutionService;
    private final com.notificationengine.repository.NotificationRepository notificationRepository;
    private final com.notificationengine.service.NotificationStateMachineService stateMachineService;

    @Value("${app.kafka.simulation-mode:true}")
    private boolean simulationMode;

    @Override
    @Transactional
    public NotificationEvent ingestEvent(EventPayload payload) {
        // 1. Validation
        validateEvent(payload);

        // Generate correlation ID if not present, or check idempotency if provided
        if (payload.getCorrelationId() == null || payload.getCorrelationId().isBlank()) {
            payload.setCorrelationId(UuidUtil.generateCorrelationId());
        } else {
            // Idempotency: Return existing event if duplicate correlationId is received
            Optional<NotificationEvent> existing = eventRepository.findByCorrelationId(payload.getCorrelationId());
            if (existing.isPresent()) {
                log.info("[INGESTION] Idempotency match: Duplicate event detected for correlationId={}. Returning existing event id={}.",
                        payload.getCorrelationId(), existing.get().getId());
                return existing.get();
            }
        }

        // 2. Persist event payload with processed = false
        NotificationEvent event = NotificationEvent.builder()
                .eventType(payload.getEventType().toUpperCase())
                .userId(payload.getUserId())
                .source(payload.getSource())
                .correlationId(payload.getCorrelationId())
                .payload(JsonUtil.toJson(payload))
                .priority(EventPriority.valueOf(payload.getPriority().name()))
                .processed(false)
                .build();

        NotificationEvent saved;
        try {
            saved = eventRepository.save(event);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            log.warn("[INGESTION] Concurrent duplicate correlationId={} detected on atomic save. Returning existing event.",
                    payload.getCorrelationId());
            return eventRepository.findByCorrelationId(payload.getCorrelationId())
                    .orElseThrow(() -> dive);
        }
        log.info("[INGESTION] Event ingested successfully: id={}, type={}, correlationId={}",
                saved.getId(), saved.getEventType(), saved.getCorrelationId());

        // 3. Trigger asynchronous publishing
        publishEventAsync(saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public int recoverUnprocessedEvents() {
        List<NotificationEvent> unprocessed = eventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        if (unprocessed == null || unprocessed.isEmpty()) {
            return 0;
        }
        int count = 0;
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(15);
        for (NotificationEvent event : unprocessed) {
            if (event.getCreatedAt() != null && event.getCreatedAt().isBefore(threshold)) {
                log.warn("[INGESTION-RECOVERY] Resuming unprocessed event from prior run: id={}, type={}, correlationId={}",
                        event.getId(), event.getEventType(), event.getCorrelationId());
                publishEventAsync(event.getId());
                count++;
            }
        }
        return count;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        int recovered = recoverUnprocessedEvents();
        if (recovered > 0) {
            log.info("[INGESTION-RECOVERY] Successfully resumed {} unprocessed events on application startup.", recovered);
        }
    }

    @Override
    @Async("notificationTaskExecutor")
    @Transactional
    public void publishEventAsync(UUID eventId) {
        log.debug("[INGESTION-ASYNC] Asynchronously publishing event: id={}", eventId);
        
        NotificationEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("[INGESTION-ASYNC] Event not found for async publishing: id={}", eventId);
                    return new IllegalArgumentException("Event not found: " + eventId);
                });

        try {
            String jsonPayload = event.getPayload();

            if (simulationMode) {
                log.info("[INGESTION-ASYNC] Simulation mode enabled. Publishing event to simulated bus.");
                simulatedEventBus.publish(event.getEventType(), jsonPayload);
            } else {
                log.info("[INGESTION-ASYNC] Simulation mode disabled. Publishing event to Kafka.");
                if (kafkaProducer.isPresent()) {
                    kafkaProducer.get().sendNotificationEvent(event.getUserId(), jsonPayload);
                } else {
                    log.warn("[INGESTION-ASYNC] Kafka producer is not configured, but simulation mode is false. Falling back to simulated bus.");
                    simulatedEventBus.publish(event.getEventType(), jsonPayload);
                }
            }

            // Publish to Spring internal bus (triggers local notification event listeners)
            applicationEventPublisher.publishEvent(new DomainNotificationEvent(this, event));

            log.info("[INGESTION-ASYNC] Event published successfully: id={}", event.getId());
        } catch (Exception e) {
            log.error("[INGESTION-ASYNC] Failed to publish event: id={}. Error: {}", event.getId(), e.getMessage());
            event.setErrorMessage(e.getMessage());
            eventRepository.save(event);
        }
    }

    @Override
    @Transactional
    public void processEvent(EventPayload payload, UUID eventId) {
        log.info("[INGESTION-PROCESS] Processing event: type={}, user={}", payload.getEventType(), payload.getUserId());

        // Resolve channels
        List<ChannelType> channels = getTargetChannels(payload);
        log.info("[INGESTION-PROCESS] Target channels resolved: {}", channels);

        UUID lastNotificationId = null;
        int successCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        for (ChannelType channel : channels) {
            try {
                String recipientAddress = resolveRecipientAddress(payload.getUserId(), channel);

                // 1. Create NotificationEntity early to track states
                com.notificationengine.entity.NotificationEntity entity = com.notificationengine.entity.NotificationEntity.builder()
                        .userId(payload.getUserId())
                        .recipientAddress(recipientAddress)
                        .channelType(channel)
                        .status(com.notificationengine.entity.NotificationEntity.NotificationStatus.CREATED)
                        .eventType(payload.getEventType())
                        .priority(payload.getPriority().name())
                        .build();
                entity = notificationRepository.save(entity);
                
                // Transition to ROUTED
                stateMachineService.transitionState(entity, com.notificationengine.entity.NotificationEntity.NotificationStatus.ROUTED, "Routed by Channel Routing Engine");

                // Find taxonomy event to pass to compliance engine
                FinancialEventType taxonomy = Arrays.stream(FinancialEventType.values())
                        .filter(e -> e.getEventCode().equalsIgnoreCase(payload.getEventType()) || e.name().equalsIgnoreCase(payload.getEventType()))
                        .findFirst()
                        .orElse(FinancialEventType.TXNX_001);

                // DND and Compliance Validation
                boolean isCompliant = dndComplianceEngine.validateCompliance(payload.getUserId(), recipientAddress, channel, taxonomy);
                if (!isCompliant) {
                    log.warn("[INGESTION-PROCESS] Notification blocked by DND Compliance Engine: channel={}, user={}, recipient={}",
                            channel, payload.getUserId(), com.notificationengine.util.NotificationUtil.maskRecipient(recipientAddress, channel));
                    stateMachineService.transitionState(entity, com.notificationengine.entity.NotificationEntity.NotificationStatus.DND, "Blocked by TRAI DND Compliance");
                    errorMessages.append("[").append(channel).append(": BLOCKED_BY_COMPLIANCE]");
                    continue; // Skip this channel
                }

                // Frequency Capping Validation
                boolean isWithinCaps = frequencyCapService.isWithinCaps(payload.getUserId(), channel, payload.getEventType(), payload.getPriority().name());
                if (!isWithinCaps) {
                    log.warn("[INGESTION-PROCESS] Notification blocked by Frequency Capping Engine: channel={}, user={}, eventType={}",
                            channel, payload.getUserId(), payload.getEventType());
                    stateMachineService.transitionState(entity, com.notificationengine.entity.NotificationEntity.NotificationStatus.CAPPED, "Blocked by Frequency Capping");
                    errorMessages.append("[").append(channel).append(": BLOCKED_BY_FREQ_CAP]");
                    continue; // Skip this channel
                }

                // Pipeline Step: Enrich payload via PersonalizationService
                PersonalizedPayload personalized = personalizationService.personalize(
                        payload.getUserId(),
                        payload.getEventType(),
                        payload.getData()
                );
                
                stateMachineService.transitionState(entity, com.notificationengine.entity.NotificationEntity.NotificationStatus.ENRICHED, "Payload personalized and enriched");

                // Fetch resolved preferences to get quiet hours
                ResolvedPreference prefs = preferenceResolutionService.resolvePreference(payload.getUserId(), payload.getEventType(), channel);

                // Quiet Hours Check
                boolean isCritical = "CRITICAL".equalsIgnoreCase(payload.getPriority().name());
                if (!isCritical && quietHoursService.isQuietHour(prefs.getQuietHoursStart(), prefs.getQuietHoursEnd(), personalized.getTimezone())) {
                    LocalDateTime nextRunAt = quietHoursService.calculateNextRunAt(prefs.getQuietHoursEnd(), personalized.getTimezone());
                    log.info("[INGESTION-PROCESS] Notification caught in quiet hours. Queueing until {}: channel={}, user={}",
                            nextRunAt, channel, payload.getUserId());
                    
                    stateMachineService.transitionState(entity, com.notificationengine.entity.NotificationEntity.NotificationStatus.QUIET, "Queued due to quiet hours");

                    NotificationRequest requestToQueue = NotificationRequest.builder()
                            .notificationId(entity.getId()) // Link the existing entity
                            .userId(payload.getUserId())
                            .recipientAddress(recipientAddress)
                            .channelType(channel)
                            .eventType(payload.getEventType())
                            .templateName(payload.getEventType())
                            .templateVariables(personalized.getVariables())
                            .priority(payload.getPriority().name())
                            .correlationId(payload.getCorrelationId())
                            .metadata(payload.getMetadata())
                            .build();

                    queuedNotificationService.queueForQuietHours(requestToQueue, nextRunAt);
                    errorMessages.append("[").append(channel).append(": QUEUED_FOR_QUIET_HOURS]");
                    continue; // Skip immediate sending
                }

                NotificationRequest request = NotificationRequest.builder()
                        .notificationId(entity.getId()) // Link the existing entity
                        .userId(payload.getUserId())
                        .recipientAddress(recipientAddress)
                        .channelType(channel)
                        .eventType(payload.getEventType())
                        .templateName(payload.getEventType())
                        .templateVariables(personalized.getVariables()) // use personalized vars
                        .priority(payload.getPriority().name())
                        .correlationId(payload.getCorrelationId())
                        .metadata(payload.getMetadata())
                        .build();

                log.info("[INGESTION-PROCESS] Dispatching notification request: channel={}, user={}, address={}",
                        channel, payload.getUserId(), recipientAddress);

                NotificationResponse response = notificationService.send(request);
                lastNotificationId = response.getId();
                successCount++;
            } catch (Exception e) {
                log.error("[INGESTION-PROCESS] Failed to send notification for channel {}: {}", channel, e.getMessage());
                errorMessages.append("[").append(channel).append(": ").append(e.getMessage()).append("]");
            }
        }

        // Update status of the NotificationEvent
        if (eventId != null) {
            NotificationEvent event = eventRepository.findById(eventId).orElse(null);
            if (event != null) {
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                if (lastNotificationId != null) {
                    event.setNotificationId(lastNotificationId);
                }
                if (errorMessages.length() > 0) {
                    event.setErrorMessage(errorMessages.toString());
                }
                eventRepository.save(event);
                log.info("[INGESTION-PROCESS] NotificationEvent updated in event store: id={}, processed=true, successCount={}",
                        eventId, successCount);
            }
        }
    }

    private List<ChannelType> getTargetChannels(EventPayload payload) {
        // If explicit target channels are specified in the payload, honour them
        if (payload.getTargetChannels() != null && payload.getTargetChannels().length > 0) {
            return Arrays.asList(payload.getTargetChannels());
        }

        // Delegate to the Intelligent Channel Routing Engine
        return channelRoutingService.determineOptimalChannels(
                payload.getUserId(),
                payload.getEventType(),
                payload.getPriority() != null ? payload.getPriority().name() : "NORMAL"
        );
    }

    private String resolveRecipientAddress(String userId, ChannelType channel) {
        return switch (channel) {
            case EMAIL    -> userId + "@example.com";
            case SMS      -> "+120255501" + String.format("%02d", Math.abs(userId.hashCode() % 100));
            case PUSH     -> "device_token_" + userId + "_" + UuidUtil.generateCorrelationId().substring(0, 8);
            case WHATSAPP -> "+120255501" + String.format("%02d", Math.abs(userId.hashCode() % 100));
            case IN_APP   -> userId;
            case SLACK    -> "U" + String.format("%08d", Math.abs(userId.hashCode() % 100000000));
            case WEBHOOK  -> "https://example.com/webhook/" + userId;
            default       -> userId;
        };
    }

    private void validateEvent(EventPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Event payload cannot be null");
        }
        if (payload.getEventType() == null || payload.getEventType().isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (payload.getUserId() == null || payload.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        // Validate eventType against FinancialEventType taxonomy
        boolean isValidTaxonomy = Arrays.stream(FinancialEventType.values())
                .anyMatch(t -> t.getEventCode().equalsIgnoreCase(payload.getEventType()) 
                        || t.name().equalsIgnoreCase(payload.getEventType()));

        if (!isValidTaxonomy) {
            throw new IllegalArgumentException("Invalid event type: '" + payload.getEventType() 
                    + "'. Event type must match a valid Financial Event Type code (e.g., TXNX-001) or name (e.g., TXNX_001).");
        }
    }
}
