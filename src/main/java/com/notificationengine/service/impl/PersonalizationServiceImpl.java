package com.notificationengine.service.impl;

import com.notificationengine.dto.PersonalizedPayload;
import com.notificationengine.entity.UserProfile;
import com.notificationengine.personalization.*;
import com.notificationengine.service.PersonalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Full implementation of the Personalization Pipeline.
 *
 * <h3>Pipeline Steps</h3>
 * <ol>
 *   <li><b>User Profile Resolution</b> — DB lookup with synthetic fallback</li>
 *   <li><b>Locale Resolution</b> — profile → preference → "en" chain</li>
 *   <li><b>Financial Calculations</b> — P&amp;L, portfolio impact, risk level</li>
 *   <li><b>Currency Formatting</b> — Indian-locale formatting for INR amounts</li>
 *   <li><b>Date Formatting</b> — locale+timezone-aware date strings</li>
 *   <li><b>Variable Merge</b> — raw event vars + all derived vars merged into final map</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizationServiceImpl implements PersonalizationService {

    private final UserProfileResolver profileResolver;
    private final LocaleResolver localeResolver;
    private final TimezoneResolver timezoneResolver;
    private final CurrencyFormatter currencyFormatter;
    private final DateFormatter dateFormatter;
    private final FinancialCalculator financialCalculator;

    @Override
    public PersonalizedPayload personalize(String userId, String eventType, Map<String, String> rawVariables) {
        log.debug("[PERSONALIZATION] Starting pipeline for userId={} eventType={}", userId, eventType);

        Map<String, String> vars = rawVariables != null ? new HashMap<>(rawVariables) : new HashMap<>();

        // ── Step 1: Resolve user profile ────────────────────────────────────
        UserProfile profile = profileResolver.resolve(userId);

        // ── Step 2: Resolve locale ───────────────────────────────────────────
        String localeCode = localeResolver.resolveLocaleCode(profile);
        Locale javaLocale = localeResolver.resolveJavaLocale(profile);
        String currency   = profile.getPreferredCurrency();

        // ── Step 3: Derive financial calculations ────────────────────────────
        BigDecimal pnl              = financialCalculator.calculatePnl(profile);
        BigDecimal pnlPercent       = financialCalculator.calculatePnlPercent(profile);
        String tradeAmount          = financialCalculator.extractTradeAmount(vars);
        BigDecimal portfolioImpact  = financialCalculator.calculatePortfolioImpact(tradeAmount, profile);
        String riskExposureLevel    = financialCalculator.deriveRiskExposureLevel(profile, pnlPercent);
        BigDecimal marginUtilPct    = financialCalculator.calculateMarginUtilisation(profile);

        // ── Step 4: Format currency values ───────────────────────────────────
        String fmtPortfolio = currencyFormatter.format(profile.getPortfolioValue(), currency, javaLocale);
        String fmtPnl       = currencyFormatter.formatWithSign(pnl, currency, javaLocale);
        String fmtPnlPct    = currencyFormatter.formatPercent(pnlPercent);
        String fmtMargin    = currencyFormatter.format(profile.getAvailableMargin(), currency, javaLocale);

        // Format any raw amounts in the event variables
        formatRawAmounts(vars, currency, javaLocale);

        // ── Step 5: Format date values ───────────────────────────────────────
        formatRawDates(vars, profile, javaLocale);

        // ── Step 6: Build the enriched variable map ──────────────────────────
        // Identity
        vars.put("userName",         displayName(profile));
        vars.put("displayName",      displayName(profile));
        vars.put("firstName",        firstName(profile));
        vars.put("userId",           userId);

        // Locale / timezone context
        vars.put("userLocale",       localeCode);
        vars.put("userTimezone",     profile.getTimezone());
        vars.put("userCurrency",     currency);
        vars.put("currentDate",      dateFormatter.today(profile, javaLocale));
        vars.put("currentDateTime",  dateFormatter.nowDateTime(profile, javaLocale));
        vars.put("currentYear",      String.valueOf(java.time.Year.now().getValue()));
        vars.put("appName",          "NotificationEngine");

        // Formatted portfolio
        vars.put("portfolioValue",         fmtPortfolio);
        vars.put("formattedPortfolioValue", fmtPortfolio);
        vars.put("pnl",                    fmtPnl);
        vars.put("formattedPnl",           fmtPnl);
        vars.put("pnlPercent",             fmtPnlPct);
        vars.put("formattedPnlPercent",    fmtPnlPct);
        vars.put("availableMargin",        fmtMargin);
        vars.put("formattedMargin",        fmtMargin);

        // Derived risk / portfolio data
        vars.put("riskProfile",         profile.getRiskProfile() != null ? profile.getRiskProfile().name() : "MODERATE");
        vars.put("riskExposureLevel",   riskExposureLevel);
        vars.put("portfolioImpact",     portfolioImpact.toPlainString() + "%");
        vars.put("marginUtilisation",   marginUtilPct.toPlainString() + "%");
        vars.put("accountType",         profile.getAccountType() != null ? profile.getAccountType().name() : "COMBINED");
        vars.put("kycStatus",           profile.getKycStatus() != null ? profile.getKycStatus().name() : "VERIFIED");

        log.info("[PERSONALIZATION] Pipeline complete for userId={} locale={} risk={} pnl={}",
                userId, localeCode, riskExposureLevel, fmtPnl);

        return PersonalizedPayload.builder()
                .userId(userId)
                .displayName(displayName(profile))
                .firstName(firstName(profile))
                .locale(localeCode)
                .timezone(profile.getTimezone())
                .currency(currency)
                .riskProfile(profile.getRiskProfile())
                .portfolioValue(profile.getPortfolioValue())
                .investedAmount(profile.getInvestedAmount())
                .availableMargin(profile.getAvailableMargin())
                .pnl(pnl)
                .pnlPercent(pnlPercent)
                .portfolioImpactPercent(portfolioImpact)
                .riskExposureLevel(riskExposureLevel)
                .formattedPortfolioValue(fmtPortfolio)
                .formattedPnl(fmtPnl)
                .formattedPnlPercent(fmtPnlPct)
                .formattedAvailableMargin(fmtMargin)
                .variables(vars)
                .build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Formats known numeric amount keys in-place. */
    private void formatRawAmounts(Map<String, String> vars, String currency, Locale locale) {
        String[] amountKeys = {"price", "totalAmount", "amount", "netAmount",
                               "requiredAmount", "shortfall", "sipAmount", "dividend",
                               "currentMargin", "brokerage", "stt", "totalCharges"};
        for (String key : amountKeys) {
            if (vars.containsKey(key) && !vars.get(key).isBlank()) {
                String formatted = currencyFormatter.format(vars.get(key), currency, locale);
                vars.put(key, formatted);
                // also publish a "formatted" variant
                vars.put("formatted" + capitalize(key), formatted);
            }
        }
    }

    /** Converts known timestamp/date keys to user timezone. */
    private void formatRawDates(Map<String, String> vars, UserProfile profile, Locale locale) {
        String[] dateKeys = {"orderDate", "tradeDate", "expiryDate", "sipDate",
                             "deadline", "contractDate", "settlementDate", "createdAt"};
        for (String key : dateKeys) {
            if (vars.containsKey(key) && !vars.get(key).isBlank()) {
                String formatted = timezoneResolver.convertToUserTimezone(vars.get(key), profile, locale);
                vars.put(key, formatted);
            }
        }
    }

    private String displayName(UserProfile p) {
        if (p.getDisplayName() != null && !p.getDisplayName().isBlank()) return p.getDisplayName();
        if (p.getFirstName() != null && !p.getFirstName().isBlank()) return p.getFirstName();
        return "Valued Customer";
    }

    private String firstName(UserProfile p) {
        if (p.getFirstName() != null && !p.getFirstName().isBlank()) return p.getFirstName();
        return displayName(p).split(" ")[0];
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
