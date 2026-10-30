package com.example.medicinereminder;

import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class HistoryDatesTest {
    private long time(int year, int month, int day, int hour) {
        Calendar c = Calendar.getInstance(); c.clear(); c.set(year, month - 1, day, hour, 0);
        return c.getTimeInMillis();
    }
    @Test public void dateKeyIncludesYearMonthDay() {
        assertEquals("2026-01-02", HistoryDates.dayKey(time(2026, 1, 2, 8)));
    }
    @Test public void sevenDaysIncludesTodayAndPreviousSixDays() {
        assertEquals("2025-12-28", HistoryDates.firstDay(time(2026, 1, 3, 12), 7));
    }
    @Test public void todayFilterStaysOnSameDay() {
        assertEquals("2026-10-30", HistoryDates.firstDay(time(2026, 10, 30, 23), 1));
    }
    @Test(expected = IllegalArgumentException.class) public void invalidRangeRejected() {
        HistoryDates.firstDay(System.currentTimeMillis(), 0);
    }
    @Test public void daylightSavingDayUsesCalendarMidnightNotFixed24Hours() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            long now = time(2026, 3, 8, 12);
            assertEquals(23 * 60 * 60 * 1000L,
                    HistoryDates.boundary(now, 1) - HistoryDates.boundary(now, 0));
        } finally { TimeZone.setDefault(original); }
    }
}
