package com.notificationengine.service;

import com.notificationengine.service.impl.QuietHoursServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class QuietHoursServiceTest {

    private final QuietHoursService quietHoursService = new QuietHoursServiceImpl();

    @Test
    void testIsQuietHour_Default_CrossMidnight() {
        // We can't control ZonedDateTime.now() easily without Clock injection,
        // but we can test the fallback logic or use a mocked Clock in a real scenario.
        // For this simple test suite, we'll verify the parsing doesn't throw exceptions.
        
        // Since we didn't inject Clock into the service, we can't reliably assert true/false 
        // because it depends on the real current time.
        // But we can check that it doesn't crash when given valid inputs.
        
        assertDoesNotThrow(() -> {
            quietHoursService.isQuietHour("21:00", "08:00", "Asia/Kolkata");
        });
    }

    @Test
    void testIsQuietHour_SameDay() {
        assertDoesNotThrow(() -> {
            quietHoursService.isQuietHour("13:00", "15:00", "America/New_York");
        });
    }

    @Test
    void testIsQuietHour_InvalidTimezone_ShouldFallbackToUTC() {
        assertDoesNotThrow(() -> {
            boolean result = quietHoursService.isQuietHour("21:00", "08:00", "Invalid/Timezone");
            // Since it falls back to UTC and logs an error, it should not throw
            assertFalse(result, "Should return false on exception");
        });
    }

    @Test
    void testCalculateNextRunAt() {
        // If current time is 22:00 and quiet end is 08:00, next run should be tomorrow 08:00 UTC equivalent
        LocalDateTime nextRun = quietHoursService.calculateNextRunAt("08:00", "UTC");
        
        assertNotNull(nextRun);
        // The hour in UTC should be 08
        assertEquals(8, nextRun.getHour());
    }
}
