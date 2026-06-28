package com.notificationengine.repository;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link NotificationTemplate}.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByName(String name);

    Optional<NotificationTemplate> findByNameAndActive(String name, boolean active);

    List<NotificationTemplate> findByChannelTypeAndActive(ChannelType channelType, boolean active);

    List<NotificationTemplate> findByEventTypeAndActive(String eventType, boolean active);

    Optional<NotificationTemplate> findByChannelTypeAndEventTypeAndActive(
            ChannelType channelType, String eventType, boolean active);

    Page<NotificationTemplate> findByActive(boolean active, Pageable pageable);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.active = true " +
           "AND (:channelType IS NULL OR t.channelType = :channelType) " +
           "AND (:eventType IS NULL OR t.eventType = :eventType)")
    Page<NotificationTemplate> findActiveTemplates(
            @Param("channelType") ChannelType channelType,
            @Param("eventType") String eventType,
            Pageable pageable);

    boolean existsByName(String name);

    long countByChannelTypeAndActive(ChannelType channelType, boolean active);

    // ------- Locale-aware lookup -------

    /**
     * Finds the active template for an exact channel + eventType + locale match.
     */
    Optional<NotificationTemplate> findByChannelTypeAndEventTypeAndLocaleAndActive(
            ChannelType channelType, String eventType, String locale, boolean active);

    /**
     * Finds all active templates for a given channel + eventType across any locale,
     * ordered so that the "en" fallback locale appears last.
     */
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE t.active = true " +
           "AND t.channelType = :channelType " +
           "AND t.eventType = :eventType " +
           "ORDER BY t.locale ASC")
    List<NotificationTemplate> findAllByChannelTypeAndEventTypeAndActive(
            @Param("channelType") ChannelType channelType,
            @Param("eventType") String eventType);

    /**
     * Locale-aware search with fallback: tries exact locale first, falls back to "en".
     */
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE t.active = true " +
           "AND t.channelType = :channelType " +
           "AND t.eventType = :eventType " +
           "AND (t.locale = :locale OR t.locale = 'en') " +
           "ORDER BY CASE WHEN t.locale = :locale THEN 0 ELSE 1 END ASC")
    List<NotificationTemplate> findByChannelEventLocaleWithFallback(
            @Param("channelType") ChannelType channelType,
            @Param("eventType") String eventType,
            @Param("locale") String locale);
}
