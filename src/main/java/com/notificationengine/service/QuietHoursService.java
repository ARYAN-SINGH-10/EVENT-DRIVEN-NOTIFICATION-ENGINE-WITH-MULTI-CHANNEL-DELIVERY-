package com.notificationengine.service;

import java.time.LocalDateTime;

public interface QuietHoursService {

    /**
     * Checks if the current time falls within the configured quiet hours for the user's timezone.
     *
     * @param quietStart The start time (HH:mm) - defaults to 21:00 if null/blank
     * @param quietEnd   The end time (HH:mm) - defaults to 08:00 if null/blank
     * @param timezone   The user's IANA timezone (e.g. Asia/Kolkata) - defaults to UTC if null/blank
     * @return true if currently in quiet hours, false otherwise
     */
    boolean isQuietHour(String quietStart, String quietEnd, String timezone);

    /**
     * Calculates the next exact moment the quiet hours end in UTC time.
     * Use this to schedule a queued notification.
     *
     * @param quietEnd The end time (HH:mm)
     * @param timezone The user's timezone
     * @return The UTC LocalDateTime when quiet hours end
     */
    LocalDateTime calculateNextRunAt(String quietEnd, String timezone);
}
