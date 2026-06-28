package com.notificationengine.service;

import com.notificationengine.analytics.AnalyticsAggregator;
import com.notificationengine.channel.ChannelProcessorFactory;
import com.notificationengine.channel.NotificationChannelProcessor;
import com.notificationengine.dlq.DeadLetterQueueHandler;
import com.notificationengine.dto.EventPayload;
import com.notificationengine.dto.NotificationRequest;
import com.notificationengine.dto.PersonalizedPayload;
import com.notificationengine.dto.ResolvedPreference;
import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.entity.NotificationEvent;
import com.notificationengine.entity.NotificationEvent.EventPriority;
import com.notificationengine.preference.PreferenceEvaluator;
import com.notificationengine.repository.NotificationEventRepository;
import com.notificationengine.repository.NotificationRepository;
import com.notificationengine.repository.NotificationTemplateRepository;
import com.notificationengine.service.impl.NotificationEventServiceImpl;
import com.notificationengine.service.impl.NotificationServiceImpl;
import com.notificationengine.template.TemplateEngine;
import com.notificationengine.template.TemplateVariableResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailureScenarioTest {

    // NotificationServiceImpl dependencies
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationTemplateRepository templateRepository;
    @Mock private ChannelProcessorFactory channelProcessorFactory;
    @Mock private NotificationChannelProcessor channelProcessor;
    @Mock private PreferenceEvaluator preferenceEvaluator;
    @Mock private TemplateEngine templateEngine;
    @Mock private TemplateVariableResolver variableResolver;
    @Mock private AnalyticsAggregator analyticsAggregator;
    @Mock private DeadLetterQueueHandler dlqHandler;
    @Mock private NotificationStateMachineService stateMachineService;

    // NotificationEventServiceImpl dependencies
    @Mock private NotificationEventRepository eventRepository;
    @Mock private com.notificationengine.event.SimulatedEventBus simulatedEventBus;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private ChannelRoutingService channelRoutingService;
    @Mock private PersonalizationService personalizationService;
    @Mock private DndComplianceEngine dndComplianceEngine;
    @Mock private FrequencyCapService frequencyCapService;
    @Mock private QuietHoursService quietHoursService;
    @Mock private QueuedNotificationService queuedNotificationService;
    @Mock private PreferenceResolutionService preferenceResolutionService;

    private NotificationServiceImpl notificationService;
    private NotificationEventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                notificationRepository, templateRepository, channelProcessorFactory,
                preferenceEvaluator, templateEngine, variableResolver,
                analyticsAggregator, dlqHandler, stateMachineService
        );
        ReflectionTestUtils.setField(notificationService, "initialRetryDelayMs", 30000L);
        ReflectionTestUtils.setField(notificationService, "retryBackoffMultiplier", 2.0);

        eventService = new NotificationEventServiceImpl(
                eventRepository, simulatedEventBus, Optional.empty(), applicationEventPublisher,
                notificationService, channelRoutingService, personalizationService,
                dndComplianceEngine, frequencyCapService, quietHoursService,
                queuedNotificationService, preferenceResolutionService, notificationRepository, stateMachineService
        );
    }

    @Test
    void testProviderFailureAndRetryLogic() {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setChannelType(ChannelType.SMS);
        notification.setMaxRetries(3);
        notification.setRetryCount(0);

        when(stateMachineService.transitionState(any(), any(), anyString())).thenAnswer(i -> {
            NotificationEntity e = i.getArgument(0);
            e.setStatus(i.getArgument(1));
            return e;
        });
        when(channelProcessorFactory.getProcessor(ChannelType.SMS)).thenReturn(channelProcessor);
        when(channelProcessor.send(any())).thenThrow(new RuntimeException("Provider Timeout"));

        ReflectionTestUtils.invokeMethod(notificationService, "dispatch", notification);

        assertEquals(NotificationStatus.RETRYING, notification.getStatus());
        assertEquals(1, notification.getRetryCount());
        assertNotNull(notification.getScheduledAt());
        verify(analyticsAggregator).recordDeliveryEvent(eq(ChannelType.SMS), any(), eq(NotificationStatus.FAILED), any());
    }

    @Test
    void testDlqRoutingWhenMaxRetriesExceeded() {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setChannelType(ChannelType.EMAIL);
        notification.setMaxRetries(3);
        notification.setRetryCount(3); // Already at max retries

        when(stateMachineService.transitionState(any(), any(), anyString())).thenAnswer(i -> {
            NotificationEntity e = i.getArgument(0);
            e.setStatus(i.getArgument(1));
            return e;
        });
        when(channelProcessorFactory.getProcessor(ChannelType.EMAIL)).thenReturn(channelProcessor);
        when(channelProcessor.send(any())).thenThrow(new RuntimeException("Provider Hard Failure"));

        ReflectionTestUtils.invokeMethod(notificationService, "dispatch", notification);

        assertEquals(NotificationStatus.DLQ, notification.getStatus());
        verify(dlqHandler).enqueue(eq(notification), anyString());
    }

    @Test
    void testDndBlocking() {
        EventPayload payload = new EventPayload();
        payload.setUserId("user123");
        payload.setEventType("TXNX-001");
        payload.setPriority(EventPriority.NORMAL);

        when(channelRoutingService.determineOptimalChannels(anyString(), anyString(), anyString()))
                .thenReturn(List.of(ChannelType.SMS));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(dndComplianceEngine.validateCompliance(anyString(), anyString(), eq(ChannelType.SMS), any()))
                .thenReturn(false); // Blocks due to DND
        
        eventService.processEvent(payload, UUID.randomUUID());

        // Verify status transition to DND
        ArgumentCaptor<NotificationStatus> statusCaptor = ArgumentCaptor.forClass(NotificationStatus.class);
        verify(stateMachineService, times(2)).transitionState(any(), statusCaptor.capture(), anyString());
        
        List<NotificationStatus> states = statusCaptor.getAllValues();
        assertEquals(NotificationStatus.ROUTED, states.get(0));
        assertEquals(NotificationStatus.DND, states.get(1));
        
        verifyNoInteractions(personalizationService); // Should skip the rest of the pipeline
    }

    @Test
    void testFrequencyCapEnforcement() {
        EventPayload payload = new EventPayload();
        payload.setUserId("user123");
        payload.setEventType("TXNX-001");
        payload.setPriority(EventPriority.NORMAL);

        when(channelRoutingService.determineOptimalChannels(anyString(), anyString(), anyString()))
                .thenReturn(List.of(ChannelType.EMAIL));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(dndComplianceEngine.validateCompliance(anyString(), anyString(), any(), any())).thenReturn(true);
        when(frequencyCapService.isWithinCaps(anyString(), eq(ChannelType.EMAIL), anyString(), anyString()))
                .thenReturn(false); // Blocked by frequency cap
        
        eventService.processEvent(payload, UUID.randomUUID());

        ArgumentCaptor<NotificationStatus> statusCaptor = ArgumentCaptor.forClass(NotificationStatus.class);
        verify(stateMachineService, times(2)).transitionState(any(), statusCaptor.capture(), anyString());
        
        List<NotificationStatus> states = statusCaptor.getAllValues();
        assertEquals(NotificationStatus.CAPPED, states.get(1));
        
        verifyNoInteractions(personalizationService);
    }

    @Test
    void testQuietHourQueueing() {
        EventPayload payload = new EventPayload();
        payload.setUserId("user123");
        payload.setEventType("TXNX-001");
        payload.setPriority(EventPriority.NORMAL);

        when(channelRoutingService.determineOptimalChannels(anyString(), anyString(), anyString()))
                .thenReturn(List.of(ChannelType.PUSH));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(dndComplianceEngine.validateCompliance(anyString(), anyString(), any(), any())).thenReturn(true);
        when(frequencyCapService.isWithinCaps(anyString(), any(), anyString(), anyString())).thenReturn(true);
        
        PersonalizedPayload pPayload = new PersonalizedPayload();
        pPayload.setTimezone("America/New_York");
        when(personalizationService.personalize(anyString(), anyString(), any())).thenReturn(pPayload);
        
        ResolvedPreference prefs = new ResolvedPreference();
        prefs.setQuietHoursStart("22:00");
        prefs.setQuietHoursEnd("08:00");
        when(preferenceResolutionService.resolvePreference(anyString(), anyString(), any())).thenReturn(prefs);
        
        when(quietHoursService.isQuietHour(anyString(), anyString(), anyString())).thenReturn(true);
        when(quietHoursService.calculateNextRunAt(anyString(), anyString())).thenReturn(java.time.LocalDateTime.now().plusHours(5));

        eventService.processEvent(payload, UUID.randomUUID());

        ArgumentCaptor<NotificationStatus> statusCaptor = ArgumentCaptor.forClass(NotificationStatus.class);
        verify(stateMachineService, times(3)).transitionState(any(), statusCaptor.capture(), anyString());
        
        List<NotificationStatus> states = statusCaptor.getAllValues();
        assertEquals(NotificationStatus.QUIET, states.get(2));
        
        verify(queuedNotificationService).queueForQuietHours(any(NotificationRequest.class), any());
    }
}
