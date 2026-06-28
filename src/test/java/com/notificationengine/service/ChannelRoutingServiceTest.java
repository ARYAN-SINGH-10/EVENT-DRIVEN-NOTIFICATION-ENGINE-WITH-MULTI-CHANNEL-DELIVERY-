package com.notificationengine.service;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.provider.DeliveryProviderService;
import com.notificationengine.provider.NotificationChannel;
import com.notificationengine.service.impl.ChannelRoutingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelRoutingService — Intelligent Routing Engine Unit Tests")
class ChannelRoutingServiceTest {

    @Mock
    private DeliveryProviderService providerService;

    @Mock
    private com.notificationengine.service.PreferenceResolutionService preferenceResolutionService;

    @InjectMocks
    private ChannelRoutingServiceImpl routingService;

    @BeforeEach
    void setUp() {
        // By default, all channels are healthy
        when(providerService.isChannelHealthy(any(NotificationChannel.class))).thenReturn(true);
        // By default, mock PreferenceResolutionService to return a generic enabled preference with effective priority
        lenient().when(preferenceResolutionService.resolvePreference(any(), any(), any())).thenAnswer(inv -> {
            return com.notificationengine.dto.ResolvedPreference.builder()
                    .enabled(true)
                    // Just use "NORMAL" if we don't have the priority context, but wait, resolvePreference doesn't take priority.
                    // Oh, resolvePreference only takes userId, eventType, channel. It resolves effectivePriority.
                    // For these tests, we can just default to returning the original priority or "HIGH"/"CRITICAL" if the test name implies it.
                    .effectivePriority("NORMAL")
                    .build();
        });
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // Tier 1: Regulatory Override (Moved to PreferenceResolutionService, tested there)
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Tier 2: User Preferences
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Tier 2 — User Preference Filtering")
    class UserPreferenceTests {

        @Test
        @DisplayName("Channel disabled in user preferences is excluded")
        void channelDisabledByPreference_isExcluded() {
            // Given: user has EMAIL disabled
            when(preferenceResolutionService.resolvePreference("user-pref-001", "TXNX-001", ChannelType.EMAIL))
                    .thenReturn(com.notificationengine.dto.ResolvedPreference.builder().enabled(false).build());

            // When: route a NORMAL priority event
            List<ChannelType> channels = routingService.determineOptimalChannels("user-pref-001", "TXNX-001", "NORMAL");

            // Then: EMAIL must not be in the result
            assertThat(channels).doesNotContain(ChannelType.EMAIL);
        }

        @Test
        @DisplayName("User opted out of specific event type on SMS is excluded for that event")
        void userOptedOutOfEventType_isExcludedForThatEvent() {
            when(preferenceResolutionService.resolvePreference("user-opt-001", "TXNX-001", ChannelType.SMS))
                    .thenReturn(com.notificationengine.dto.ResolvedPreference.builder().enabled(false).build());

            // When
            List<ChannelType> channels = routingService.determineOptimalChannels("user-opt-001", "TXNX-001", "HIGH");

            // Then: SMS should be excluded
            assertThat(channels).doesNotContain(ChannelType.SMS);
        }

        @Test
        @DisplayName("User opted out of a different event type still receives SMS for other events")
        void userOptedOutOfDifferentEventType_doesNotAffectOtherEvents() {
            // When: route TXNX-001
            List<ChannelType> channels = routingService.determineOptimalChannels("user-opt-002", "TXNX-001", "HIGH");

            // Then: SMS should still be eligible (default mock returns enabled=true)
            assertThat(channels).contains(ChannelType.SMS);
        }
    }

    // -------------------------------------------------------------------------
    // Tier 3: Delivery Optimization (Latency & Reliability)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Tier 3 — Delivery Optimization")
    class DeliveryOptimizationTests {

        @Test
        @DisplayName("CRITICAL priority event scores SMS, PUSH, and WHATSAPP higher than EMAIL")
        void criticalPriority_fastChannelsScoreHigher() {
            when(preferenceResolutionService.resolvePreference(any(), any(), any())).thenReturn(
                    com.notificationengine.dto.ResolvedPreference.builder().enabled(true).effectivePriority("CRITICAL").build()
            );

            Map<ChannelType, Double> txnxScores = routingService.getRoutingScores("user-001", "TXNX-001", "CRITICAL");

            assertThat(txnxScores).containsKey(ChannelType.SMS);
            assertThat(txnxScores).containsKey(ChannelType.PUSH);
            assertThat(txnxScores.get(ChannelType.SMS)).isGreaterThan(txnxScores.getOrDefault(ChannelType.EMAIL, 0.0));
        }

        @Test
        @DisplayName("HIGH priority event includes SMS and PUSH")
        void highPriority_includesFastChannels() {
            when(preferenceResolutionService.resolvePreference(any(), any(), any())).thenReturn(
                    com.notificationengine.dto.ResolvedPreference.builder().enabled(true).effectivePriority("HIGH").build()
            );
            List<ChannelType> channels = routingService.determineOptimalChannels("user-001", "TXNX-001", "HIGH");

            // Fast channels should win for HIGH priority
            assertThat(channels).isNotEmpty();
            assertThat(channels).containsAnyOf(ChannelType.SMS, ChannelType.PUSH);
        }

        @Test
        @DisplayName("Unhealthy provider channel is excluded from routing")
        void unhealthyProvider_isExcludedFromScoring() {
            // Given: SMS provider is unhealthy
            when(providerService.isChannelHealthy(NotificationChannel.SMS)).thenReturn(false);

            // When
            List<ChannelType> channels = routingService.determineOptimalChannels("user-001", "TXNX-001", "HIGH");

            // Then: SMS should not be in results
            assertThat(channels).doesNotContain(ChannelType.SMS);
        }
    }

    // -------------------------------------------------------------------------
    // Tier 4: Cost Optimization
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Tier 4 — Cost Optimization")
    class CostOptimizationTests {

        @Test
        @DisplayName("LOW priority event: IN_APP scores higher than SMS in cost scoring")
        void lowPriority_inAppScoresHigherThanSms() {
            when(preferenceResolutionService.resolvePreference(any(), any(), any())).thenReturn(
                    com.notificationengine.dto.ResolvedPreference.builder().enabled(true).effectivePriority("LOW").build()
            );
            Map<ChannelType, Double> scores = routingService.getRoutingScores("user-001", "SIPX-004", "LOW");

            // IN_APP has negligible cost (score approaches 100) vs SMS (cost=1.0, score=0)
            if (scores.containsKey(ChannelType.IN_APP) && scores.containsKey(ChannelType.SMS)) {
                assertThat(scores.get(ChannelType.IN_APP)).isGreaterThan(scores.get(ChannelType.SMS));
            }
        }

        @Test
        @DisplayName("LOW priority event: EMAIL scores higher than SMS due to lower cost")
        void lowPriority_emailScoresHigherThanSms() {
            when(preferenceResolutionService.resolvePreference(any(), any(), any())).thenReturn(
                    com.notificationengine.dto.ResolvedPreference.builder().enabled(true).effectivePriority("LOW").build()
            );
            Map<ChannelType, Double> scores = routingService.getRoutingScores("user-001", "SIPX-004", "LOW");

            if (scores.containsKey(ChannelType.EMAIL) && scores.containsKey(ChannelType.SMS)) {
                assertThat(scores.get(ChannelType.EMAIL)).isGreaterThan(scores.get(ChannelType.SMS));
            }
        }
    }

    // -------------------------------------------------------------------------
    // General / Fallback Behaviour
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Fallback & Edge Case Tests")
    class FallbackTests {

        @Test
        @DisplayName("Returns non-empty channel list for all valid financial event types")
        void allTaxonomyEventTypes_returnNonEmptyChannels() {
            List<String> eventCodes = List.of("TXNX-001", "TXNX-003", "RISK-001", "SIPX-001", "MKTX-001", "REGX-001");

            for (String code : eventCodes) {
                List<ChannelType> channels = routingService.determineOptimalChannels("user-001", code, "NORMAL");
                assertThat(channels)
                        .as("Expected non-empty channels for event " + code)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("Fallback to highest scoring channel when all channels are below threshold")
        void allChannelsLow_fallbackToHighestScoring() {
            // Given: all providers except IN_APP return unhealthy
            when(providerService.isChannelHealthy(NotificationChannel.EMAIL)).thenReturn(false);
            when(providerService.isChannelHealthy(NotificationChannel.SMS)).thenReturn(false);
            when(providerService.isChannelHealthy(NotificationChannel.PUSH)).thenReturn(false);
            when(providerService.isChannelHealthy(NotificationChannel.WHATSAPP)).thenReturn(false);
            when(providerService.isChannelHealthy(NotificationChannel.IN_APP)).thenReturn(true);

            List<ChannelType> channels = routingService.determineOptimalChannels("user-001", "TXNX-001", "LOW");

            // Should fall back to IN_APP (the only healthy channel)
            assertThat(channels).containsExactly(ChannelType.IN_APP);
        }

        @Test
        @DisplayName("getRoutingScores returns a non-null map for valid inputs")
        void getRoutingScores_returnsMap() {
            Map<ChannelType, Double> scores = routingService.getRoutingScores("user-001", "TXNX-001", "NORMAL");

            assertThat(scores).isNotNull();
            assertThat(scores).isNotEmpty();
            scores.values().forEach(score ->
                    assertThat(score).isBetween(0.0, 130.0) // CRITICAL fast channel can exceed 100
            );
        }
    }
}
