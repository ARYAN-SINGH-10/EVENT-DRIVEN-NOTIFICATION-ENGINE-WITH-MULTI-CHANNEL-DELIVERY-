package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rich user profile used by the Personalization Pipeline to resolve locale,
 * timezone, currency, and financial context for every outbound notification.
 *
 * <p>Data here is never used for authentication — it is purely a personalization
 * store that the notification engine queries at dispatch time.</p>
 */
@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_up_user_id", columnList = "user_id", unique = true),
        @Index(name = "idx_up_locale", columnList = "locale"),
        @Index(name = "idx_up_risk_profile", columnList = "risk_profile")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Matches the userId used everywhere else in the engine. */
    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    // -----------------------------------------------------------------------
    // Localization preferences
    // -----------------------------------------------------------------------

    /**
     * ISO 639-1 language code (e.g. "en", "hi", "mr", "ta", "te").
     * Drives template locale selection and number/date formatting.
     */
    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "en";

    /**
     * IANA timezone ID (e.g. "Asia/Kolkata", "Asia/Kolkata", "America/New_York").
     * Used to convert UTC timestamps in notifications to local time.
     */
    @Column(name = "timezone", length = 60)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    /**
     * ISO 4217 currency code (e.g. "INR", "USD").
     * Drives currency formatting in financial notifications.
     */
    @Column(name = "preferred_currency", length = 5)
    @Builder.Default
    private String preferredCurrency = "INR";

    // -----------------------------------------------------------------------
    // Financial profile
    // -----------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile", length = 20)
    @Builder.Default
    private RiskProfile riskProfile = RiskProfile.MODERATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20)
    @Builder.Default
    private AccountType accountType = AccountType.COMBINED;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", length = 20)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.VERIFIED;

    /** Current total portfolio market value (INR). */
    @Column(name = "portfolio_value", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal portfolioValue = BigDecimal.ZERO;

    /** Total amount invested (cost basis, INR). */
    @Column(name = "invested_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal investedAmount = BigDecimal.ZERO;

    /** Available margin / cash balance (INR). */
    @Column(name = "available_margin", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availableMargin = BigDecimal.ZERO;

    /** Used margin (INR). */
    @Column(name = "used_margin", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal usedMargin = BigDecimal.ZERO;

    /** Whether this is a premium / wealth management account. */
    @Column(name = "premium_account")
    @Builder.Default
    private boolean premiumAccount = false;

    // -----------------------------------------------------------------------
    // Metadata
    // -----------------------------------------------------------------------

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -----------------------------------------------------------------------
    // Enumerations
    // -----------------------------------------------------------------------

    public enum RiskProfile {
        CONSERVATIVE, MODERATE, AGGRESSIVE
    }

    public enum AccountType {
        DEMAT, TRADING, COMBINED, NRI
    }

    public enum KycStatus {
        VERIFIED, PENDING, EXPIRED, REJECTED
    }
}
