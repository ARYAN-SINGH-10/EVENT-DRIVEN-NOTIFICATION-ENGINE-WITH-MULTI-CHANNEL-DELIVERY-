package com.notificationengine.repository;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.UserPreference;
import com.notificationengine.entity.UserPreference.NotificationFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link UserPreference}.
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    List<UserPreference> findByUserId(String userId);

    Optional<UserPreference> findByUserIdAndChannelType(String userId, ChannelType channelType);

    List<UserPreference> findByUserIdAndEnabled(String userId, boolean enabled);

    @Query("SELECT up FROM UserPreference up WHERE up.userId = :userId AND up.enabled = true")
    List<UserPreference> findEnabledPreferencesForUser(@Param("userId") String userId);

    @Query("SELECT up FROM UserPreference up WHERE up.channelType = :channelType AND up.enabled = true")
    List<UserPreference> findEnabledUsersForChannel(@Param("channelType") ChannelType channelType);

    boolean existsByUserIdAndChannelType(String userId, ChannelType channelType);

    @Query("SELECT up FROM UserPreference up WHERE up.userId = :userId " +
           "AND up.enabled = true AND up.frequency = :frequency")
    List<UserPreference> findByUserIdAndFrequency(
            @Param("userId") String userId,
            @Param("frequency") NotificationFrequency frequency);

    void deleteByUserId(String userId);

    long countByUserId(String userId);

    long countByEnabledTrue();

    long countByEnabledFalse();
}
