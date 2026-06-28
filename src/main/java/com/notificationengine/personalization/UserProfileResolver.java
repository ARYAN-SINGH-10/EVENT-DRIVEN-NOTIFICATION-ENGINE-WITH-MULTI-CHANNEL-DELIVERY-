package com.notificationengine.personalization;

import com.notificationengine.entity.UserProfile;
import com.notificationengine.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Resolves a {@link UserProfile} for a given userId.
 * <p>
 * If no profile exists in the database, a synthetic default profile is
 * generated so the personalization pipeline never blocks notification delivery.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileResolver {

    private final UserProfileRepository profileRepository;

    /**
     * Returns a UserProfile for the given userId.
     * Falls back to a synthetic default if no record is found.
     */
    @Cacheable(value = "userProfiles", key = "#userId")
    public UserProfile resolve(String userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.debug("[PERSONALIZATION] No profile found for userId={}, using synthetic default", userId);
                    return buildSyntheticProfile(userId);
                });
    }

    private UserProfile buildSyntheticProfile(String userId) {
        return UserProfile.builder()
                .userId(userId)
                .displayName(capitalizeUserId(userId))
                .firstName(capitalizeUserId(userId))
                .locale("en")
                .timezone("Asia/Kolkata")
                .preferredCurrency("INR")
                .riskProfile(UserProfile.RiskProfile.MODERATE)
                .accountType(UserProfile.AccountType.COMBINED)
                .kycStatus(UserProfile.KycStatus.VERIFIED)
                .portfolioValue(BigDecimal.ZERO)
                .investedAmount(BigDecimal.ZERO)
                .availableMargin(BigDecimal.ZERO)
                .usedMargin(BigDecimal.ZERO)
                .build();
    }

    private String capitalizeUserId(String userId) {
        if (userId == null || userId.isBlank()) return "Valued Customer";
        String clean = userId.replace("_", " ").replace("-", " ");
        String[] parts = clean.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
