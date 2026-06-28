package com.notificationengine.controller;

import com.notificationengine.entity.UserProfile;
import com.notificationengine.entity.UserProfile.AccountType;
import com.notificationengine.entity.UserProfile.KycStatus;
import com.notificationengine.entity.UserProfile.RiskProfile;
import com.notificationengine.repository.UserProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
@Tag(name = "User Profile API", description = "CRUD for user personalization profiles")
public class UserProfileController {

    private final UserProfileRepository profileRepository;

    @PostMapping
    @Operation(summary = "Create a new user profile")
    public ResponseEntity<UserProfile> create(@RequestBody UserProfileRequest request) {
        UserProfile profile = UserProfile.builder()
                .userId(request.getUserId())
                .displayName(request.getDisplayName())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .locale(request.getLocale() != null ? request.getLocale() : "en")
                .timezone(request.getTimezone() != null ? request.getTimezone() : "Asia/Kolkata")
                .preferredCurrency(request.getPreferredCurrency() != null ? request.getPreferredCurrency() : "INR")
                .riskProfile(request.getRiskProfile() != null ? request.getRiskProfile() : RiskProfile.MODERATE)
                .accountType(request.getAccountType() != null ? request.getAccountType() : AccountType.COMBINED)
                .kycStatus(request.getKycStatus() != null ? request.getKycStatus() : KycStatus.VERIFIED)
                .portfolioValue(request.getPortfolioValue() != null ? request.getPortfolioValue() : BigDecimal.ZERO)
                .investedAmount(request.getInvestedAmount() != null ? request.getInvestedAmount() : BigDecimal.ZERO)
                .availableMargin(request.getAvailableMargin() != null ? request.getAvailableMargin() : BigDecimal.ZERO)
                .usedMargin(request.getUsedMargin() != null ? request.getUsedMargin() : BigDecimal.ZERO)
                .premiumAccount(request.isPremiumAccount())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(profileRepository.save(profile));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile by userId")
    public ResponseEntity<UserProfile> getByUserId(@PathVariable String userId) {
        return profileRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all user profiles")
    public ResponseEntity<List<UserProfile>> getAll() {
        return ResponseEntity.ok(profileRepository.findAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user profile by database ID")
    public ResponseEntity<UserProfile> update(@PathVariable UUID id, @RequestBody UserProfileRequest request) {
        return profileRepository.findById(id).map(profile -> {
            if (request.getDisplayName() != null) profile.setDisplayName(request.getDisplayName());
            if (request.getLocale() != null)       profile.setLocale(request.getLocale());
            if (request.getTimezone() != null)     profile.setTimezone(request.getTimezone());
            if (request.getRiskProfile() != null)  profile.setRiskProfile(request.getRiskProfile());
            if (request.getPortfolioValue() != null) profile.setPortfolioValue(request.getPortfolioValue());
            if (request.getInvestedAmount() != null) profile.setInvestedAmount(request.getInvestedAmount());
            if (request.getAvailableMargin() != null) profile.setAvailableMargin(request.getAvailableMargin());
            if (request.getUsedMargin() != null)   profile.setUsedMargin(request.getUsedMargin());
            return ResponseEntity.ok(profileRepository.save(profile));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete a user profile by userId")
    public ResponseEntity<Void> delete(@PathVariable String userId) {
        profileRepository.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class UserProfileRequest {
        private String userId;
        private String displayName;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String locale;
        private String timezone;
        private String preferredCurrency;
        private RiskProfile riskProfile;
        private AccountType accountType;
        private KycStatus kycStatus;
        private BigDecimal portfolioValue;
        private BigDecimal investedAmount;
        private BigDecimal availableMargin;
        private BigDecimal usedMargin;
        private boolean premiumAccount;
    }
}
