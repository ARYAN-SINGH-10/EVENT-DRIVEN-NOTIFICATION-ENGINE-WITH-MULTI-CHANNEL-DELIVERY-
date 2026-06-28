package com.notificationengine.personalization;

import com.notificationengine.entity.UserPreference;
import com.notificationengine.entity.UserProfile;
import com.notificationengine.repository.UserPreferenceRepository;
import com.notificationengine.template.TemplateLocale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Resolves the best-fit {@link Locale} and locale code for a user.
 *
 * <h3>Resolution chain (highest priority first):</h3>
 * <ol>
 *   <li>UserProfile.locale (explicit user setting)</li>
 *   <li>Any enabled UserPreference record that has a locale set</li>
 *   <li>English ("en") as the final fallback</li>
 * </ol>
 */
@Slf4j
@Component("personalizationLocaleResolver")
@RequiredArgsConstructor
public class LocaleResolver {

    private final UserPreferenceRepository preferenceRepository;

    /**
     * Resolves the locale code string (e.g. "hi", "en") for a user.
     */
    public String resolveLocaleCode(UserProfile profile) {
        // Tier 1: UserProfile locale
        if (StringUtils.isNotBlank(profile.getLocale())) {
            return TemplateLocale.fromCode(profile.getLocale()).getCode();
        }

        // Tier 2: UserPreference locale (any channel)
        List<UserPreference> prefs = preferenceRepository.findEnabledPreferencesForUser(profile.getUserId());
        for (UserPreference pref : prefs) {
            if (StringUtils.isNotBlank(pref.getLocale())) {
                String code = pref.getLocale().contains("-")
                        ? pref.getLocale().split("-")[0]
                        : pref.getLocale();
                return TemplateLocale.fromCode(code).getCode();
            }
        }

        // Tier 3: English fallback
        return TemplateLocale.EN.getCode();
    }

    /**
     * Returns a Java {@link Locale} object for number/date formatting.
     * Uses Indian locale for INR currency formatting.
     */
    public Locale resolveJavaLocale(UserProfile profile) {
        String localeCode = resolveLocaleCode(profile);
        String currency = profile.getPreferredCurrency();

        // Use Indian locale for INR (correct lakh/crore grouping)
        if ("INR".equalsIgnoreCase(currency) || "IN".equalsIgnoreCase(localeCode)) {
            return new Locale("en", "IN");
        }

        return switch (localeCode) {
            case "hi" -> new Locale("hi", "IN");
            case "mr" -> new Locale("mr", "IN");
            case "ta" -> new Locale("ta", "IN");
            case "te" -> new Locale("te", "IN");
            default   -> Locale.US;
        };
    }
}
