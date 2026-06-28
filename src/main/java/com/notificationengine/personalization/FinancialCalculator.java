package com.notificationengine.personalization;

import com.notificationengine.entity.UserProfile;
import com.notificationengine.entity.UserProfile.RiskProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Derives key financial metrics from user profile and raw event variables.
 *
 * <h3>Calculations performed:</h3>
 * <ul>
 *   <li><b>P&amp;L</b> = portfolioValue − investedAmount</li>
 *   <li><b>P&amp;L %</b> = (P&amp;L / investedAmount) × 100</li>
 *   <li><b>Portfolio Impact %</b> = (tradeAmount / portfolioValue) × 100</li>
 *   <li><b>Risk Exposure Level</b> = composite of riskProfile + pnlPercent + usedMargin%</li>
 *   <li><b>Margin Utilisation %</b> = (usedMargin / (usedMargin + availableMargin)) × 100</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * Computes P&L based on current portfolio and invested amounts from the user profile.
     *
     * @return P&L value (positive = profit, negative = loss)
     */
    public BigDecimal calculatePnl(UserProfile profile) {
        BigDecimal portfolio = profile.getPortfolioValue();
        BigDecimal invested = profile.getInvestedAmount();
        if (portfolio == null || invested == null) return BigDecimal.ZERO;
        return portfolio.subtract(invested).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Computes P&L percentage relative to invested amount.
     */
    public BigDecimal calculatePnlPercent(UserProfile profile) {
        BigDecimal invested = profile.getInvestedAmount();
        if (invested == null || invested.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal pnl = calculatePnl(profile);
        return pnl.divide(invested, 4, RoundingMode.HALF_UP)
                   .multiply(HUNDRED)
                   .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Computes what percentage of total portfolio the given trade amount represents.
     *
     * @param tradeAmountStr raw trade amount string (e.g. "15250.00")
     * @param profile        user profile with portfolio value
     */
    public BigDecimal calculatePortfolioImpact(String tradeAmountStr, UserProfile profile) {
        if (tradeAmountStr == null) return BigDecimal.ZERO;
        BigDecimal portfolio = profile.getPortfolioValue();
        if (portfolio == null || portfolio.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        try {
            BigDecimal trade = new BigDecimal(tradeAmountStr.replaceAll("[,\\s₹$€£]", ""));
            return trade.divide(portfolio, 4, RoundingMode.HALF_UP)
                        .multiply(HUNDRED)
                        .setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Computes margin utilisation percentage.
     */
    public BigDecimal calculateMarginUtilisation(UserProfile profile) {
        BigDecimal used = profile.getUsedMargin();
        BigDecimal available = profile.getAvailableMargin();
        if (used == null || available == null) return BigDecimal.ZERO;
        BigDecimal total = used.add(available);
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return used.divide(total, 4, RoundingMode.HALF_UP)
                   .multiply(HUNDRED)
                   .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Derives a human-readable risk exposure level from multiple signals.
     *
     * @return "CRITICAL" | "HIGH" | "MODERATE" | "LOW"
     */
    public String deriveRiskExposureLevel(UserProfile profile, BigDecimal pnlPercent) {
        RiskProfile rp = profile.getRiskProfile() != null ? profile.getRiskProfile() : RiskProfile.MODERATE;
        BigDecimal marginUtil = calculateMarginUtilisation(profile);

        // High margin utilisation is always risky
        if (marginUtil.compareTo(BigDecimal.valueOf(80)) >= 0) return "CRITICAL";

        // Severe loss territory
        if (pnlPercent.compareTo(BigDecimal.valueOf(-20)) <= 0) return "CRITICAL";
        if (pnlPercent.compareTo(BigDecimal.valueOf(-10)) <= 0) return "HIGH";

        return switch (rp) {
            case CONSERVATIVE -> pnlPercent.compareTo(BigDecimal.valueOf(-5)) <= 0 ? "HIGH" : "MODERATE";
            case MODERATE     -> pnlPercent.compareTo(BigDecimal.valueOf(-15)) <= 0 ? "HIGH" : "MODERATE";
            case AGGRESSIVE   -> "LOW";
        };
    }

    /**
     * Extracts a numeric trade amount from raw event variables.
     * Tries keys: "totalAmount", "amount", "price" × "quantity".
     */
    public String extractTradeAmount(Map<String, String> variables) {
        if (variables == null) return "0";

        if (variables.containsKey("totalAmount")) return variables.get("totalAmount");
        if (variables.containsKey("amount"))      return variables.get("amount");
        if (variables.containsKey("netAmount"))   return variables.get("netAmount");

        // Derive from price × quantity
        String price = variables.get("price");
        String qty   = variables.get("quantity");
        if (price != null && qty != null) {
            try {
                BigDecimal p = new BigDecimal(price.replaceAll("[,\\s₹$€£]", ""));
                BigDecimal q = new BigDecimal(qty.replaceAll("[,\\s]", ""));
                return p.multiply(q, MC).setScale(2, RoundingMode.HALF_UP).toPlainString();
            } catch (NumberFormatException ignored) {}
        }

        return "0";
    }
}
