package com.notificationengine.repository;

import com.notificationengine.entity.NotificationChannel;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link NotificationChannel}.
 */
@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, UUID> {

    Optional<NotificationChannel> findByChannelType(ChannelType channelType);

    List<NotificationChannel> findByActive(boolean active);

    Optional<NotificationChannel> findByChannelTypeAndActive(ChannelType channelType, boolean active);

    boolean existsByChannelType(ChannelType channelType);

    @Modifying
    @Query("UPDATE NotificationChannel c SET c.active = :active WHERE c.channelType = :channelType")
    int toggleChannelStatus(@Param("channelType") ChannelType channelType, @Param("active") boolean active);

    @Query("SELECT c FROM NotificationChannel c WHERE c.active = true AND c.simulationMode = false")
    List<NotificationChannel> findActiveNonSimulatedChannels();
}
