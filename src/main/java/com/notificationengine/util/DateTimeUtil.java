package com.notificationengine.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date/time utility methods.
 */
public final class DateTimeUtil {

    private DateTimeUtil() {}

    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Returns current UTC time.
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneId.of("UTC"));
    }

    /**
     * Converts a LocalDateTime from UTC to the given timezone.
     */
    public static ZonedDateTime toZone(LocalDateTime utcTime, String timezone) {
        return utcTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(timezone));
    }

    /**
     * Returns true if the given time falls within the quiet hours window.
     *
     * @param currentTime  current local time in user's timezone
     * @param quietStart   quiet hours start e.g. "22:00"
     * @param quietEnd     quiet hours end e.g. "07:00"
     */
    public static boolean isWithinQuietHours(LocalTime currentTime, String quietStart, String quietEnd) {
        if (quietStart == null || quietEnd == null) return false;

        LocalTime start = LocalTime.parse(quietStart);
        LocalTime end   = LocalTime.parse(quietEnd);

        if (start.isBefore(end)) {
            // Normal range e.g. 09:00 - 18:00
            return !currentTime.isBefore(start) && !currentTime.isAfter(end);
        } else {
            // Overnight range e.g. 22:00 - 07:00
            return !currentTime.isBefore(start) || !currentTime.isAfter(end);
        }
    }

    /**
     * Calculates the next retry time using exponential backoff.
     *
     * @param baseDelayMs     initial delay in ms
     * @param failureCount    current number of failures
     * @param multiplier      backoff multiplier
     */
    public static LocalDateTime calculateNextRetry(long baseDelayMs, int failureCount, double multiplier) {
        long delayMs = (long) (baseDelayMs * Math.pow(multiplier, failureCount - 1));
        delayMs = Math.min(delayMs, 3_600_000L); // cap at 1 hour
        return LocalDateTime.now().plusNanos(delayMs * 1_000_000L);
    }
}
