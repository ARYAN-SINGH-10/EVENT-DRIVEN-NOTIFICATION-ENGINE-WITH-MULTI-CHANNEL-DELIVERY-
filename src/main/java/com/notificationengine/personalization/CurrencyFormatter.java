package com.notificationengine.personalization;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Formats monetary amounts using the user's locale and currency code.
 *
 * <p>For INR (Indian Rupee), uses the Indian number grouping system
 * (lakhs and crores): ₹1,23,456.78 rather than ₹123,456.78.</p>
 */
@Slf4j
@Component
public class CurrencyFormatter {

    /**
     * Formats a numeric string as a locale-aware currency amount.
     *
     * @param rawAmount   numeric string (e.g. "152500.50")
     * @param currencyCode ISO 4217 code (e.g. "INR", "USD")
     * @param javaLocale  Java locale driving number grouping
     * @return formatted string (e.g. "₹1,52,500.50") or rawAmount if unparseable
     */
    public String format(String rawAmount, String currencyCode, Locale javaLocale) {
        if (StringUtils.isBlank(rawAmount)) return rawAmount;
        try {
            BigDecimal amount = new BigDecimal(rawAmount.replaceAll("[,\\s]", ""));
            return format(amount, currencyCode, javaLocale);
        } catch (NumberFormatException e) {
            log.debug("[CURRENCY] Cannot parse amount '{}': {}", rawAmount, e.getMessage());
            return rawAmount;
        }
    }

    /**
     * Formats a BigDecimal as a locale-aware currency string.
     */
    public String format(BigDecimal amount, String currencyCode, Locale javaLocale) {
        if (amount == null) return "0.00";
        try {
            NumberFormat nf = NumberFormat.getCurrencyInstance(javaLocale);
            nf.setCurrency(Currency.getInstance(currencyCode));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(amount);
        } catch (Exception e) {
            log.warn("[CURRENCY] Formatting failed for amount={} currency={}: {}", amount, currencyCode, e.getMessage());
            return currencyCode + " " + amount.toPlainString();
        }
    }

    /**
     * Formats a BigDecimal with explicit sign (+ for positive, - for negative).
     */
    public String formatWithSign(BigDecimal amount, String currencyCode, Locale javaLocale) {
        if (amount == null) return "0.00";
        String formatted = format(amount, currencyCode, javaLocale);
        return amount.compareTo(BigDecimal.ZERO) >= 0 ? "+" + formatted : formatted;
    }

    /**
     * Formats a percentage value (e.g. 3.45 → "+3.45%").
     */
    public String formatPercent(BigDecimal percent) {
        if (percent == null) return "0.00%";
        String sign = percent.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + String.format("%.2f%%", percent.doubleValue());
    }

    /**
     * Parses a raw numeric string into a BigDecimal safely.
     */
    public BigDecimal parseSafe(String rawAmount) {
        if (StringUtils.isBlank(rawAmount)) return BigDecimal.ZERO;
        try {
            return new BigDecimal(rawAmount.replaceAll("[,\\s₹$€£]", ""));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
