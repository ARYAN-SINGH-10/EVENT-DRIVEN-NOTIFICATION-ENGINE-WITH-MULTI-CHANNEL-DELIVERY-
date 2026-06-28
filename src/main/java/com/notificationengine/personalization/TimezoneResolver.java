package com.notificationengine.personalization;

import com.notificationengine.entity.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Converts UTC timestamps in event variables to the user's local timezone
 * and formats them using the user's preferred locale.
 *
 * <p>Handles ISO-8601 strings (e.g. {@code "2024-06-28T10:30:00Z"}) as well
 * as date-only strings (e.g. {@code "2024-06-28"}).</p>
 */
@Slf4j
@Component
public class TimezoneResolver {

    private static final DateTimeFormatter ISO_DATETIME  = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter ISO_DATE      = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_ONLY_OUT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_OUT  = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm z");

    /**
     * Converts a raw ISO timestamp or date string to the user's local timezone
     * and returns it in a human-readable format.
     *
     * @param rawValue  ISO date or datetime string
     * @param profile   user profile supplying timezone and locale
     * @param javaLocale Java locale for month names
     * @return formatted local string, or rawValue unchanged if parsing fails
     */
    public String convertToUserTimezone(String rawValue, UserProfile profile, Locale javaLocale) {
        if (StringUtils.isBlank(rawValue)) return rawValue;

        ZoneId userZone = safeZone(profile.getTimezone());

        // Try datetime first
        try {
            ZonedDateTime utc = ZonedDateTime.parse(rawValue, ISO_DATETIME);
            ZonedDateTime local = utc.withZoneSameInstant(userZone);
            return DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm z", javaLocale).format(local);
        } catch (DateTimeParseException ignored) {}

        // Try date-only
        try {
            LocalDate date = LocalDate.parse(rawValue, ISO_DATE);
            return DATE_ONLY_OUT.withLocale(javaLocale).format(date);
        } catch (DateTimeParseException ignored) {}

        return rawValue; // return as-is if unparseable
    }

    /**
     * Converts the current UTC instant to the user's timezone as a formatted string.
     */
    public String nowInUserTimezone(UserProfile profile, Locale javaLocale) {
        ZoneId userZone = safeZone(profile.getTimezone());
        return DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm z", javaLocale)
                .format(ZonedDateTime.now(userZone));
    }

    /**
     * Returns a safe ZoneId — falls back to Asia/Kolkata on invalid input.
     */
    public ZoneId safeZone(String timezone) {
        if (StringUtils.isBlank(timezone)) return ZoneId.of("Asia/Kolkata");
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            log.warn("[TIMEZONE] Invalid timezone '{}', falling back to Asia/Kolkata", timezone);
            return ZoneId.of("Asia/Kolkata");
        }
    }
}
