package com.notificationengine.personalization;

import com.notificationengine.entity.UserProfile;
import com.notificationengine.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds test user profiles into the database on startup.
 */
@Slf4j
@Component
@Order(1) // Run early
@RequiredArgsConstructor
public class UserProfileDataInitializer implements CommandLineRunner {

    private final UserProfileRepository profileRepository;

    @Override
    public void run(String... args) {
        if (profileRepository.count() > 0) {
            log.info("[USER-SEEDER] Profiles already exist, skipping initialization.");
            return;
        }

        log.info("[USER-SEEDER] Seeding initial user profiles...");

        List<UserProfile> seedData = List.of(
                // 1. Hindi user, conservative, high margin utilization
                UserProfile.builder()
                        .userId("user_hi_001")
                        .displayName("Rahul Sharma")
                        .firstName("Rahul")
                        .locale("hi")
                        .timezone("Asia/Kolkata")
                        .preferredCurrency("INR")
                        .riskProfile(UserProfile.RiskProfile.CONSERVATIVE)
                        .accountType(UserProfile.AccountType.COMBINED)
                        .portfolioValue(new BigDecimal("150000.00"))
                        .investedAmount(new BigDecimal("180000.00")) // loss
                        .availableMargin(new BigDecimal("5000.00"))
                        .usedMargin(new BigDecimal("45000.00")) // 90% utilization
                        .build(),

                // 2. Marathi user, aggressive, good profit
                UserProfile.builder()
                        .userId("user_mr_002")
                        .displayName("Sneha Kulkarni")
                        .firstName("Sneha")
                        .locale("mr")
                        .timezone("Asia/Kolkata")
                        .preferredCurrency("INR")
                        .riskProfile(UserProfile.RiskProfile.AGGRESSIVE)
                        .accountType(UserProfile.AccountType.TRADING)
                        .portfolioValue(new BigDecimal("2500000.00"))
                        .investedAmount(new BigDecimal("1500000.00")) // huge profit
                        .availableMargin(new BigDecimal("500000.00"))
                        .usedMargin(new BigDecimal("100000.00"))
                        .build(),

                // 3. Tamil user, moderate
                UserProfile.builder()
                        .userId("user_ta_003")
                        .displayName("Karthik Natarajan")
                        .firstName("Karthik")
                        .locale("ta")
                        .timezone("Asia/Kolkata")
                        .preferredCurrency("INR")
                        .riskProfile(UserProfile.RiskProfile.MODERATE)
                        .portfolioValue(new BigDecimal("850000.00"))
                        .investedAmount(new BigDecimal("800000.00"))
                        .availableMargin(new BigDecimal("150000.00"))
                        .usedMargin(BigDecimal.ZERO)
                        .build(),

                // 4. Telugu user, conservative, exact break-even
                UserProfile.builder()
                        .userId("user_te_004")
                        .displayName("Lakshmi Reddy")
                        .firstName("Lakshmi")
                        .locale("te")
                        .timezone("Asia/Kolkata")
                        .preferredCurrency("INR")
                        .riskProfile(UserProfile.RiskProfile.CONSERVATIVE)
                        .portfolioValue(new BigDecimal("500000.00"))
                        .investedAmount(new BigDecimal("500000.00"))
                        .availableMargin(new BigDecimal("20000.00"))
                        .usedMargin(BigDecimal.ZERO)
                        .build(),

                // 5. English NRI user (USD)
                UserProfile.builder()
                        .userId("user_en_005")
                        .displayName("John Doe")
                        .firstName("John")
                        .locale("en-US")
                        .timezone("America/New_York")
                        .preferredCurrency("USD")
                        .riskProfile(UserProfile.RiskProfile.MODERATE)
                        .accountType(UserProfile.AccountType.NRI)
                        .portfolioValue(new BigDecimal("50000.00"))
                        .investedAmount(new BigDecimal("45000.00"))
                        .availableMargin(new BigDecimal("15000.00"))
                        .usedMargin(new BigDecimal("5000.00"))
                        .premiumAccount(true)
                        .build()
        );

        profileRepository.saveAll(seedData);
        log.info("[USER-SEEDER] Successfully seeded {} user profiles.", seedData.size());
    }
}
