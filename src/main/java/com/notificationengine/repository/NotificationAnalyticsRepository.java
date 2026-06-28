package com.notificationengine.repository;

import com.notificationengine.entity.NotificationAnalytics;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link NotificationAnalytics} with aggregation queries
 * supporting all 8 pipeline metrics.
 */
@Repository
public interface NotificationAnalyticsRepository extends JpaRepository<NotificationAnalytics, UUID> {

    List<NotificationAnalytics> findByAnalyticsDate(LocalDate date);

    Optional<NotificationAnalytics> findByChannelTypeAndEventTypeAndAnalyticsDate(
            ChannelType channelType, String eventType, LocalDate date);

    List<NotificationAnalytics> findByChannelTypeAndAnalyticsDateBetween(
            ChannelType channelType, LocalDate from, LocalDate to);

    @Query("SELECT a FROM NotificationAnalytics a WHERE a.analyticsDate BETWEEN :from AND :to " +
           "ORDER BY a.analyticsDate DESC")
    List<NotificationAnalytics> findByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT a FROM NotificationAnalytics a WHERE a.analyticsDate = :date " +
           "ORDER BY a.totalSent DESC")
    List<NotificationAnalytics> findTopChannelsByDate(@Param("date") LocalDate date);

    // -------------------------------------------------------------------------
    // Delivery Rate
    // -------------------------------------------------------------------------
    @Query("SELECT COALESCE(SUM(a.totalSent), 0) FROM NotificationAnalytics a " +
           "WHERE a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalSent(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.totalDelivered), 0) FROM NotificationAnalytics a " +
           "WHERE a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalDelivered(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.totalSent), 0) FROM NotificationAnalytics a " +
           "WHERE a.channelType = :channelType AND a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalSentByChannelAndDateRange(
            @Param("channelType") ChannelType channelType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.totalDelivered), 0) FROM NotificationAnalytics a " +
           "WHERE a.channelType = :channelType AND a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalDeliveredByChannelAndDateRange(
            @Param("channelType") ChannelType channelType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // -------------------------------------------------------------------------
    // Read Rate
    // -------------------------------------------------------------------------
    @Query("SELECT COALESCE(SUM(a.totalRead), 0) FROM NotificationAnalytics a " +
           "WHERE a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalRead(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.totalRead), 0) FROM NotificationAnalytics a " +
           "WHERE a.channelType = :channelType AND a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalReadByChannel(
            @Param("channelType") ChannelType channelType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // -------------------------------------------------------------------------
    // Click-Through Rate
    // -------------------------------------------------------------------------
    @Query("SELECT COALESCE(SUM(a.totalClicked), 0) FROM NotificationAnalytics a " +
           "WHERE a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalClicked(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.totalClicked), 0) FROM NotificationAnalytics a " +
           "WHERE a.channelType = :channelType AND a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalClickedByChannel(
            @Param("channelType") ChannelType channelType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // -------------------------------------------------------------------------
    // Failure Rate
    // -------------------------------------------------------------------------
    @Query("SELECT COALESCE(SUM(a.totalFailed), 0) FROM NotificationAnalytics a " +
           "WHERE a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalFailed(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.totalFailed), 0) FROM NotificationAnalytics a " +
           "WHERE a.channelType = :channelType AND a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalFailedByChannel(
            @Param("channelType") ChannelType channelType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // -------------------------------------------------------------------------
    // Opt-Out Rate
    // -------------------------------------------------------------------------
    @Query("SELECT COALESCE(SUM(a.totalOptOut), 0) FROM NotificationAnalytics a " +
           "WHERE a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalOptOut(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // -------------------------------------------------------------------------
    // Frequency Cap Hits
    // -------------------------------------------------------------------------
    @Query("SELECT COALESCE(SUM(a.totalCapHits), 0) FROM NotificationAnalytics a " +
           "WHERE a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalCapHits(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.totalCapHits), 0) FROM NotificationAnalytics a " +
           "WHERE a.channelType = :channelType AND a.analyticsDate BETWEEN :from AND :to")
    Long sumTotalCapHitsByChannel(
            @Param("channelType") ChannelType channelType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // -------------------------------------------------------------------------
    // Avg Delivery Time
    // -------------------------------------------------------------------------
    @Query("SELECT AVG(a.avgDeliveryTimeMs) FROM NotificationAnalytics a " +
           "WHERE a.avgDeliveryTimeMs IS NOT NULL AND a.analyticsDate BETWEEN :from AND :to")
    Double avgDeliveryTimeMs(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT AVG(a.avgDeliveryTimeMs) FROM NotificationAnalytics a " +
           "WHERE a.avgDeliveryTimeMs IS NOT NULL AND a.channelType = :channelType " +
           "AND a.analyticsDate BETWEEN :from AND :to")
    Double avgDeliveryTimeMsByChannel(
            @Param("channelType") ChannelType channelType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // -------------------------------------------------------------------------
    // Cost
    // -------------------------------------------------------------------------
    @Query("SELECT COALESCE(SUM(a.estimatedCostUsd), 0) FROM NotificationAnalytics a " +
           "WHERE a.analyticsDate BETWEEN :from AND :to")
    Double sumEstimatedCost(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.estimatedCostUsd), 0) FROM NotificationAnalytics a " +
           "WHERE a.channelType = :channelType AND a.analyticsDate BETWEEN :from AND :to")
    Double sumEstimatedCostByChannel(
            @Param("channelType") ChannelType channelType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // -------------------------------------------------------------------------
    // Daily time-series for trend charts
    // -------------------------------------------------------------------------
    @Query("SELECT a FROM NotificationAnalytics a WHERE a.channelType = :channelType " +
           "AND a.analyticsDate BETWEEN :from AND :to ORDER BY a.analyticsDate ASC")
    List<NotificationAnalytics> findTimeSeriesByChannel(
            @Param("channelType") ChannelType channelType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT a FROM NotificationAnalytics a WHERE a.eventType = :eventType " +
           "AND a.analyticsDate BETWEEN :from AND :to ORDER BY a.analyticsDate ASC")
    List<NotificationAnalytics> findTimeSeriesByEventType(
            @Param("eventType") String eventType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
