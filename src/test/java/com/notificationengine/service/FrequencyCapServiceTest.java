package com.notificationengine.service;

import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.repository.NotificationRepository;
import com.notificationengine.service.impl.FrequencyCapServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FrequencyCapServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private FrequencyCapServiceImpl frequencyCapService;

    private static final String USER_ID = "user_123";
    private static final String EVENT_TYPE = "MKTG-001";

    @BeforeEach
    void setUp() {
        // Default mocks
    }

    @Test
    void testCriticalPriorityBypassesAllCaps() {
        boolean result = frequencyCapService.isWithinCaps(USER_ID, ChannelType.SMS, EVENT_TYPE, "CRITICAL");
        assertTrue(result, "Critical priority should bypass caps");
    }

    @Test
    void testGlobalDailyCapExceeded() {
        // Cooldown passed (no last notification)
        when(notificationRepository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(eq(USER_ID), eq(EVENT_TYPE)))
                .thenReturn(Optional.empty());

        // Global count = 12 (limit is 12, so >= 12 means blocked)
        when(notificationRepository.countByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                .thenReturn(12L);

        boolean result = frequencyCapService.isWithinCaps(USER_ID, ChannelType.PUSH, EVENT_TYPE, "NORMAL");
        assertFalse(result, "Should be blocked by global daily cap");
    }

    @Test
    void testChannelCapExceeded() {
        when(notificationRepository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(eq(USER_ID), eq(EVENT_TYPE)))
                .thenReturn(Optional.empty());

        when(notificationRepository.countByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                .thenReturn(5L); // Below global 12

        // SMS cap is 5.
        when(notificationRepository.countByUserIdAndChannelTypeAndCreatedAtBetween(eq(USER_ID), eq(ChannelType.SMS), any(), any()))
                .thenReturn(5L); // Reached cap

        boolean result = frequencyCapService.isWithinCaps(USER_ID, ChannelType.SMS, EVENT_TYPE, "NORMAL");
        assertFalse(result, "Should be blocked by SMS channel cap");
    }

    @Test
    void testCategoryCapExceeded() {
        when(notificationRepository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(eq(USER_ID), eq(EVENT_TYPE)))
                .thenReturn(Optional.empty());

        when(notificationRepository.countByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                .thenReturn(2L);

        when(notificationRepository.countByUserIdAndChannelTypeAndCreatedAtBetween(eq(USER_ID), eq(ChannelType.EMAIL), any(), any()))
                .thenReturn(2L);

        // Category cap is 3
        when(notificationRepository.countByUserIdAndEventTypeAndCreatedAtBetween(eq(USER_ID), eq(EVENT_TYPE), any(), any()))
                .thenReturn(3L);

        boolean result = frequencyCapService.isWithinCaps(USER_ID, ChannelType.EMAIL, EVENT_TYPE, "NORMAL");
        assertFalse(result, "Should be blocked by category cap");
    }

    @Test
    void testCooldownPeriodExceeded() {
        // Last notification sent 10 minutes ago (Cooldown is 60 mins)
        NotificationEntity lastSent = NotificationEntity.builder()
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(notificationRepository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(eq(USER_ID), eq(EVENT_TYPE)))
                .thenReturn(Optional.of(lastSent));

        boolean result = frequencyCapService.isWithinCaps(USER_ID, ChannelType.PUSH, EVENT_TYPE, "NORMAL");
        assertFalse(result, "Should be blocked by cooldown period");
    }

    @Test
    void testWithinCapsSuccess() {
        // Cooldown passed (70 mins ago)
        NotificationEntity lastSent = NotificationEntity.builder()
                .createdAt(LocalDateTime.now().minusMinutes(70))
                .build();

        when(notificationRepository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(eq(USER_ID), eq(EVENT_TYPE)))
                .thenReturn(Optional.of(lastSent));

        when(notificationRepository.countByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                .thenReturn(1L);

        when(notificationRepository.countByUserIdAndChannelTypeAndCreatedAtBetween(eq(USER_ID), eq(ChannelType.SMS), any(), any()))
                .thenReturn(1L);

        when(notificationRepository.countByUserIdAndEventTypeAndCreatedAtBetween(eq(USER_ID), eq(EVENT_TYPE), any(), any()))
                .thenReturn(1L);

        boolean result = frequencyCapService.isWithinCaps(USER_ID, ChannelType.SMS, EVENT_TYPE, "NORMAL");
        assertTrue(result, "Should be allowed");
    }
}
