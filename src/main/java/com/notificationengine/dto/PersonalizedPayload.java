package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notificationengine.entity.UserProfile.RiskProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Enriched notification payload produced by the Personalization Pipeline.
 *
 * <p>Contains all original raw variables from the triggering event PLUS
 * personalized fields derived from the user's profile:
 * <ul>
 *   <li>Locale, timezone, formatted currency &amp; dates</li>
 *   <li>Derived P&amp;L, portfolio impact, risk exposure</li>
 *   <li>User display name and account context</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Fully personalized and enriched notification payload")
public class PersonalizedPayload {

    // -----------------------------------------------------------------------
    // Identity
    // -----------------------------------------------------------------------

    @Schema(description = "User ID")
    private String userId;

    @Schema(description = "User's display name for salutation")
    private String displayName;

    @Schema(description = "User's first name")
    private String firstName;

    // -----------------------------------------------------------------------
    // Localization context
    // -----------------------------------------------------------------------

    @Schema(description = "Resolved locale code (e.g. 'hi', 'en')")
    private String locale;

    @Schema(description = "Resolved timezone (e.g. 'Asia/Kolkata')")
    private String timezone;

    @Schema(description = "Currency code (e.g. 'INR')")
    private String currency;

    // -----------------------------------------------------------------------
    // Financial context
    // -----------------------------------------------------------------------

    @Schema(description = "User's risk profile: CONSERVATIVE / MODERATE / AGGRESSIVE")
    private RiskProfile riskProfile;

    @Schema(description = "Total portfolio market value")
    private BigDecimal portfolioValue;

    @Schema(description = "Total invested cost basis")
    private BigDecimal investedAmount;

    @Schema(description = "Available margin / cash")
    private BigDecimal availableMargin;

    // -----------------------------------------------------------------------
    // Derived financial calculations
    // -----------------------------------------------------------------------

    @Schema(description = "Unrealised P&L (portfolioValue - investedAmount)")
    private BigDecimal pnl;

    @Schema(description = "P&L as percentage of invested amount")
    private BigDecimal pnlPercent;

    @Schema(description = "Percentage impact of this event's trade amount on total portfolio")
    private BigDecimal portfolioImpactPercent;

    @Schema(description = "Risk exposure level derived from riskProfile + pnlPercent")
    private String riskExposureLevel;

    // -----------------------------------------------------------------------
    // Formatted display values (locale + timezone applied)
    // -----------------------------------------------------------------------

    @Schema(description = "Formatted portfolio value (e.g. '₹12,34,567.89')")
    private String formattedPortfolioValue;

    @Schema(description = "Formatted P&L with sign (e.g. '+₹23,456.00' / '-₹1,200.00')")
    private String formattedPnl;

    @Schema(description = "Formatted P&L percentage (e.g. '+3.45%')")
    private String formattedPnlPercent;

    @Schema(description = "Formatted available margin")
    private String formattedAvailableMargin;

    // -----------------------------------------------------------------------
    // Merged template variable map (ready to pass to TemplateEngine.render())
    // -----------------------------------------------------------------------

    @Schema(description = "Complete variable map: raw event data + personalization fields")
    @Builder.Default
    private Map<String, String> variables = new HashMap<>();
}
