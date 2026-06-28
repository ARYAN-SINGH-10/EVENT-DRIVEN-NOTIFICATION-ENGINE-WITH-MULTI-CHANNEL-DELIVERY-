package com.notificationengine.service;

import com.notificationengine.analytics.ChannelCostConfig;
import com.notificationengine.dto.AnalyticsResponse;
import com.notificationengine.dto.AnalyticsSummaryResponse;
import com.notificationengine.entity.NotificationAnalytics;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.repository.NotificationAnalyticsRepository;
import com.notificationengine.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsServiceImpl — all 8 pipeline metrics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService — 8-Metric Analytics Pipeline Tests")
class AnalyticsServiceTest {

    @Mock NotificationAnalyticsRepository analyticsRepository;
    @Mock ChannelCostConfig costConfig;

    private AnalyticsServiceImpl analyticsService;

    private final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private final LocalDate TO   = LocalDate.of(2026, 6, 28);

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(analyticsRepository, costConfig);
        // Default cost for SMS
        lenient().when(costConfig.getCostPerUnit(ChannelType.SMS)).thenReturn(0.0075);
        lenient().when(costConfig.getCostPerUnit(ChannelType.EMAIL)).thenReturn(0.001);
        lenient().when(costConfig.getCostPerUnit(ChannelType.PUSH)).thenReturn(0.0005);
        lenient().when(costConfig.getCostPerUnit(ChannelType.WHATSAPP)).thenReturn(0.005);
        lenient().when(costConfig.getCostPerUnit(ChannelType.IN_APP)).thenReturn(0.0);
    }

    // =========================================================================
    // Metric 1: Delivery Rate
    // =========================================================================

    @Nested
    @DisplayName("Metric 1 — Delivery Rate")
    class DeliveryRateTests {

        @Test
        @DisplayName("Returns correct delivery rate when sent > 0")
        void deliveryRate_calculatesCorrectly() {
            when(analyticsRepository.sumTotalDelivered(FROM, TO)).thenReturn(800L);
            when(analyticsRepository.sumTotalSent(FROM, TO)).thenReturn(1000L);

            double rate = analyticsService.getDeliveryRate(FROM, TO, null);

            assertThat(rate).isCloseTo(0.80, offset(0.001));
        }

        @Test
        @DisplayName("Returns 0.0 when no notifications were sent")
        void deliveryRate_noSent_returnsZero() {
            when(analyticsRepository.sumTotalDelivered(FROM, TO)).thenReturn(0L);
            when(analyticsRepository.sumTotalSent(FROM, TO)).thenReturn(0L);

            double rate = analyticsService.getDeliveryRate(FROM, TO, null);

            assertThat(rate).isZero();
        }

        @Test
        @DisplayName("Filters by channel when channelType is provided")
        void deliveryRate_byChannel() {
            when(analyticsRepository.sumTotalDeliveredByChannelAndDateRange(ChannelType.SMS, FROM, TO)).thenReturn(95L);
            when(analyticsRepository.sumTotalSentByChannelAndDateRange(ChannelType.SMS, FROM, TO)).thenReturn(100L);

            double rate = analyticsService.getDeliveryRate(FROM, TO, ChannelType.SMS);

            assertThat(rate).isCloseTo(0.95, offset(0.001));
        }
    }

    // =========================================================================
    // Metric 2: Read Rate
    // =========================================================================

    @Nested
    @DisplayName("Metric 2 — Read Rate")
    class ReadRateTests {

        @Test
        @DisplayName("Returns correct read rate (read / delivered)")
        void readRate_calculatesCorrectly() {
            when(analyticsRepository.sumTotalRead(FROM, TO)).thenReturn(600L);
            when(analyticsRepository.sumTotalDelivered(FROM, TO)).thenReturn(800L);

            double rate = analyticsService.getReadRate(FROM, TO, null);

            assertThat(rate).isCloseTo(0.75, offset(0.001));
        }

        @Test
        @DisplayName("Returns 0.0 when no notifications were delivered")
        void readRate_noDelivered_returnsZero() {
            when(analyticsRepository.sumTotalRead(FROM, TO)).thenReturn(0L);
            when(analyticsRepository.sumTotalDelivered(FROM, TO)).thenReturn(0L);

            double rate = analyticsService.getReadRate(FROM, TO, null);

            assertThat(rate).isZero();
        }
    }

    // =========================================================================
    // Metric 3: Click-Through Rate
    // =========================================================================

    @Nested
    @DisplayName("Metric 3 — Click-Through Rate")
    class ClickThroughRateTests {

        @Test
        @DisplayName("Returns correct CTR (clicked / sent)")
        void ctr_calculatesCorrectly() {
            when(analyticsRepository.sumTotalClicked(FROM, TO)).thenReturn(120L);
            when(analyticsRepository.sumTotalSent(FROM, TO)).thenReturn(1000L);

            double rate = analyticsService.getClickThroughRate(FROM, TO, null);

            assertThat(rate).isCloseTo(0.12, offset(0.001));
        }

        @Test
        @DisplayName("CTR per channel delegates to channel-specific query")
        void ctr_byChannel_usesChannelQuery() {
            when(analyticsRepository.sumTotalClickedByChannel(ChannelType.EMAIL, FROM, TO)).thenReturn(50L);
            when(analyticsRepository.sumTotalSentByChannelAndDateRange(ChannelType.EMAIL, FROM, TO)).thenReturn(200L);

            double rate = analyticsService.getClickThroughRate(FROM, TO, ChannelType.EMAIL);

            assertThat(rate).isCloseTo(0.25, offset(0.001));
        }
    }

    // =========================================================================
    // Metric 4: Failure Rate
    // =========================================================================

    @Nested
    @DisplayName("Metric 4 — Failure Rate")
    class FailureRateTests {

        @Test
        @DisplayName("Returns correct failure rate (failed / sent)")
        void failureRate_calculatesCorrectly() {
            when(analyticsRepository.sumTotalFailed(FROM, TO)).thenReturn(50L);
            when(analyticsRepository.sumTotalSent(FROM, TO)).thenReturn(1000L);

            double rate = analyticsService.getFailureRate(FROM, TO, null);

            assertThat(rate).isCloseTo(0.05, offset(0.001));
        }
    }

    // =========================================================================
    // Metric 5: Opt-Out Rate
    // =========================================================================

    @Nested
    @DisplayName("Metric 5 — Opt-Out Rate")
    class OptOutRateTests {

        @Test
        @DisplayName("Returns correct opt-out rate (opt-outs / sent)")
        void optOutRate_calculatesCorrectly() {
            when(analyticsRepository.sumTotalOptOut(FROM, TO)).thenReturn(30L);
            when(analyticsRepository.sumTotalSent(FROM, TO)).thenReturn(1000L);

            double rate = analyticsService.getOptOutRate(FROM, TO);

            assertThat(rate).isCloseTo(0.03, offset(0.001));
        }
    }

    // =========================================================================
    // Metric 6: Time To Delivery
    // =========================================================================

    @Nested
    @DisplayName("Metric 6 — Time To Delivery")
    class TimeToDeliveryTests {

        @Test
        @DisplayName("Returns average delivery time when data exists")
        void avgDeliveryTime_returnsValue() {
            when(analyticsRepository.avgDeliveryTimeMs(FROM, TO)).thenReturn(342.5);

            double avgMs = analyticsService.getAvgTimeToDelivery(FROM, TO, null);

            assertThat(avgMs).isCloseTo(342.5, offset(0.01));
        }

        @Test
        @DisplayName("Returns 0.0 when no delivery time data")
        void avgDeliveryTime_noData_returnsZero() {
            when(analyticsRepository.avgDeliveryTimeMs(FROM, TO)).thenReturn(null);

            double avgMs = analyticsService.getAvgTimeToDelivery(FROM, TO, null);

            assertThat(avgMs).isZero();
        }

        @Test
        @DisplayName("Per-channel delivery time uses channel-specific query")
        void avgDeliveryTime_byChannel() {
            when(analyticsRepository.avgDeliveryTimeMsByChannel(ChannelType.PUSH, FROM, TO)).thenReturn(250.0);

            double avgMs = analyticsService.getAvgTimeToDelivery(FROM, TO, ChannelType.PUSH);

            assertThat(avgMs).isCloseTo(250.0, offset(0.01));
        }
    }

    // =========================================================================
    // Metric 7: Cost Per Notification
    // =========================================================================

    @Nested
    @DisplayName("Metric 7 — Cost Per Notification")
    class CostPerNotificationTests {

        @Test
        @DisplayName("Cost per notification = totalCost / totalSent")
        void costPerNotification_calculatesCorrectly() {
            when(analyticsRepository.sumEstimatedCost(FROM, TO)).thenReturn(7.5);    // 1000 SMS at $0.0075
            when(analyticsRepository.sumTotalSent(FROM, TO)).thenReturn(1000L);

            double cost = analyticsService.getCostPerNotification(FROM, TO, null);

            assertThat(cost).isCloseTo(0.0075, offset(0.00001));
        }

        @Test
        @DisplayName("Returns 0 when no notifications sent")
        void costPerNotification_noSent_returnsZero() {
            when(analyticsRepository.sumEstimatedCost(FROM, TO)).thenReturn(0.0);
            when(analyticsRepository.sumTotalSent(FROM, TO)).thenReturn(0L);

            double cost = analyticsService.getCostPerNotification(FROM, TO, null);

            assertThat(cost).isZero();
        }

        @Test
        @DisplayName("Email is cheaper than SMS per notification")
        void emailCheaperThanSms() {
            when(analyticsRepository.sumEstimatedCostByChannel(ChannelType.EMAIL, FROM, TO)).thenReturn(1.0);    // 1000 emails
            when(analyticsRepository.sumTotalSentByChannelAndDateRange(ChannelType.EMAIL, FROM, TO)).thenReturn(1000L);

            when(analyticsRepository.sumEstimatedCostByChannel(ChannelType.SMS, FROM, TO)).thenReturn(7.5);     // 1000 SMS
            when(analyticsRepository.sumTotalSentByChannelAndDateRange(ChannelType.SMS, FROM, TO)).thenReturn(1000L);

            double emailCost = analyticsService.getCostPerNotification(FROM, TO, ChannelType.EMAIL);
            double smsCost   = analyticsService.getCostPerNotification(FROM, TO, ChannelType.SMS);

            assertThat(emailCost).isLessThan(smsCost);
        }
    }

    // =========================================================================
    // Metric 8: Frequency Cap Hits
    // =========================================================================

    @Nested
    @DisplayName("Metric 8 — Frequency Cap Hits")
    class FrequencyCapHitsTests {

        @Test
        @DisplayName("Returns total cap hits across all channels")
        void capHits_returnsTotal() {
            when(analyticsRepository.sumTotalCapHits(FROM, TO)).thenReturn(42L);

            long hits = analyticsService.getFrequencyCapHits(FROM, TO, null);

            assertThat(hits).isEqualTo(42L);
        }

        @Test
        @DisplayName("Returns 0 when no cap hits")
        void capHits_noHits_returnsZero() {
            when(analyticsRepository.sumTotalCapHits(FROM, TO)).thenReturn(0L);

            long hits = analyticsService.getFrequencyCapHits(FROM, TO, null);

            assertThat(hits).isZero();
        }

        @Test
        @DisplayName("Per-channel cap hits uses channel-specific query")
        void capHits_byChannel() {
            when(analyticsRepository.sumTotalCapHitsByChannel(ChannelType.SMS, FROM, TO)).thenReturn(15L);

            long hits = analyticsService.getFrequencyCapHits(FROM, TO, ChannelType.SMS);

            assertThat(hits).isEqualTo(15L);
        }
    }

    // =========================================================================
    // Dashboard Summary
    // =========================================================================

    @Nested
    @DisplayName("Dashboard Summary")
    class SummaryTests {

        @Test
        @DisplayName("getSummary returns all 8 metrics in one call")
        void getSummary_returnsAllMetrics() {
            when(analyticsRepository.sumTotalSent(FROM, TO)).thenReturn(1000L);
            when(analyticsRepository.sumTotalDelivered(FROM, TO)).thenReturn(900L);
            when(analyticsRepository.sumTotalFailed(FROM, TO)).thenReturn(50L);
            when(analyticsRepository.sumTotalRead(FROM, TO)).thenReturn(600L);
            when(analyticsRepository.sumTotalClicked(FROM, TO)).thenReturn(120L);
            when(analyticsRepository.sumTotalOptOut(FROM, TO)).thenReturn(10L);
            when(analyticsRepository.sumTotalCapHits(FROM, TO)).thenReturn(25L);
            when(analyticsRepository.avgDeliveryTimeMs(FROM, TO)).thenReturn(310.0);
            when(analyticsRepository.sumEstimatedCost(FROM, TO)).thenReturn(7.5);
            when(analyticsRepository.findByDateRange(FROM, TO)).thenReturn(List.of());
            // Per-channel queries return 0 for this test
            lenient().when(analyticsRepository.sumEstimatedCostByChannel(any(), any(), any())).thenReturn(0.0);
            lenient().when(analyticsRepository.sumTotalCapHitsByChannel(any(), any(), any())).thenReturn(0L);

            AnalyticsSummaryResponse summary = analyticsService.getSummary(FROM, TO);

            assertThat(summary.getTotalSent()).isEqualTo(1000L);
            assertThat(summary.getTotalDelivered()).isEqualTo(900L);
            assertThat(summary.getDeliveryRate()).isCloseTo(0.90, offset(0.001));
            assertThat(summary.getFailureRate()).isCloseTo(0.05, offset(0.001));
            assertThat(summary.getReadRate()).isCloseTo(0.667, offset(0.001));
            assertThat(summary.getClickThroughRate()).isCloseTo(0.12, offset(0.001));
            assertThat(summary.getOptOutRate()).isCloseTo(0.01, offset(0.001));
            assertThat(summary.getTotalCapHits()).isEqualTo(25L);
            assertThat(summary.getAvgDeliveryTimeMs()).isCloseTo(310.0, offset(0.1));
            assertThat(summary.getAvgCostPerNotification()).isCloseTo(0.0075, offset(0.00001));
            assertThat(summary.getFrom()).isEqualTo(FROM);
            assertThat(summary.getTo()).isEqualTo(TO);
        }
    }
}
