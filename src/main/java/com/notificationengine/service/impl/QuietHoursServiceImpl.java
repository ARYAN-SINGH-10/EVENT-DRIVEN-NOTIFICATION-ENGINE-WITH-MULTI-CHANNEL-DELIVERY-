package com.notificationengine.service.impl;

import com.notificationengine.service.QuietHoursService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDateTime;

@Slf4j
@Service
public class QuietHoursServiceImpl implements QuietHoursService {

    private static final String DEFAULT_START = "21:00";
    private static final String DEFAULT_END = "08:00";
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public boolean isQuietHour(String quietStart, String quietEnd, String timezone) {
        String startStr = (quietStart != null && !quietStart.isBlank()) ? quietStart : DEFAULT_START;
        String endStr = (quietEnd != null && !quietEnd.isBlank()) ? quietEnd : DEFAULT_END;
        String tzStr = (timezone != null && !timezone.isBlank()) ? timezone : DEFAULT_TIMEZONE;

        try {
            LocalTime start = LocalTime.parse(startStr, TIME_FORMATTER);
            LocalTime end = LocalTime.parse(endStr, TIME_FORMATTER);
            ZoneId zoneId = ZoneId.of(tzStr);

            ZonedDateTime now = ZonedDateTime.now(zoneId);
            LocalTime currentTime = now.toLocalTime();

            if (start.isBefore(end)) {
                // E.g. 13:00 to 15:00
                return !currentTime.isBefore(start) && currentTime.isBefore(end);
            } else {
                // E.g. 21:00 to 08:00 (crosses midnight)
                return !currentTime.isBefore(start) || currentTime.isBefore(end);
            }
        } catch (DateTimeParseException | java.time.zone.ZoneRulesException e) {
            log.error("[QUIET-HOURS] Failed to parse quiet hours or timezone. Defaulting to NOT quiet hour. start={} end={} tz={}", startStr, endStr, tzStr, e);
            return false;
        }
    }

    @Override
    public LocalDateTime calculateNextRunAt(String quietEnd, String timezone) {
        String endStr = (quietEnd != null && !quietEnd.isBlank()) ? quietEnd : DEFAULT_END;
        String tzStr = (timezone != null && !timezone.isBlank()) ? timezone : DEFAULT_TIMEZONE;

        try {
            LocalTime end = LocalTime.parse(endStr, TIME_FORMATTER);
            ZoneId zoneId = ZoneId.of(tzStr);

            ZonedDateTime now = ZonedDateTime.now(zoneId);
            ZonedDateTime nextRun = now.with(end);

            if (nextRun.isBefore(now) || nextRun.isEqual(now)) {
                // If it's 23:00 and end is 08:00, next run is tomorrow 08:00
                nextRun = nextRun.plusDays(1);
            }

            return nextRun.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
        } catch (Exception e) {
            log.error("[QUIET-HOURS] Failed to calculate next run. Defaulting to now + 8 hours.", e);
            return LocalDateTime.now().plusHours(8);
        }
    }
}
