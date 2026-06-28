package com.notificationengine.personalization;

import com.notificationengine.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Locale and timezone-aware date formatting for notification templates.
 *
 * <p>Produces human-readable date strings in the user's language and timezone.
 * For example, "28 जून 2024" for a Hindi user, or "28 Jun 2024" for English.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DateFormatter {

    private final TimezoneResolver timezoneResolver;

    /**
     * Formats an ISO date or datetime string into a short, human-readable date
     * in the user's locale and timezone.
     *
     * @param rawDate    ISO-8601 date or datetime string
     * @param profile    user profile for timezone/locale context
     * @param javaLocale Java locale for month/day names
     * @return formatted date string
     */
    public String formatDate(String rawDate, UserProfile profile, Locale javaLocale) {
        if (StringUtils.isBlank(rawDate)) return "";

        ZoneId zone = timezoneResolver.safeZone(profile.getTimezone());
        DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("d MMMM yyyy", javaLocale);
        DateTimeFormatter shortDateTime = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm z", javaLocale);

        // Try full datetime
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(rawDate).withZoneSameInstant(zone);
            return shortDateTime.format(zdt);
        } catch (DateTimeParseException ignored) {}

        // Try local date
        try {
            LocalDate ld = LocalDate.parse(rawDate);
            return shortDate.format(ld);
        } catch (DateTimeParseException ignored) {}

        return rawDate;
    }

    /**
     * Formats today's date in the user's timezone and locale.
     */
    public String today(UserProfile profile, Locale javaLocale) {
        ZoneId zone = timezoneResolver.safeZone(profile.getTimezone());
        LocalDate now = LocalDate.now(zone);
        return DateTimeFormatter.ofPattern("d MMMM yyyy", javaLocale).format(now);
    }

    /**
     * Returns the current date+time in the user's timezone as a readable string.
     */
    public String nowDateTime(UserProfile profile, Locale javaLocale) {
        ZoneId zone = timezoneResolver.safeZone(profile.getTimezone());
        ZonedDateTime now = ZonedDateTime.now(zone);
        return DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm z", javaLocale).format(now);
    }
}
