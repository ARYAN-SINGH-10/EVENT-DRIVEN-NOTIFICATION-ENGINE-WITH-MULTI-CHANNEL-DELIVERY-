package com.notificationengine.repository;

import com.notificationengine.entity.UserProfile;
import com.notificationengine.entity.UserProfile.RiskProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link UserProfile}.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);

    List<UserProfile> findByLocale(String locale);

    List<UserProfile> findByRiskProfile(RiskProfile riskProfile);

    @Query("SELECT up FROM UserProfile up WHERE up.userId IN :userIds")
    List<UserProfile> findByUserIds(@Param("userIds") List<String> userIds);

    void deleteByUserId(String userId);
}
