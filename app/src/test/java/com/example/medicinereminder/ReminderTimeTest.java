package com.example.medicinereminder;

import org.junit.Test;
import java.util.Calendar;
import static org.junit.Assert.*;

public class ReminderTimeTest {
    private long time(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.clear(); c.set(year, month - 1, day, hour, minute);
        return c.getTimeInMillis();
    }
    @Test public void futureTimeStaysToday() {
        assertEquals(time(2026, 10, 30, 9, 0),
                ReminderTime.next(9, 0, time(2026, 10, 30, 8, 59)));
    }
    @Test public void equalMinuteMovesToTomorrow() {
        assertEquals(time(2026, 10, 31, 9, 0),
                ReminderTime.next(9, 0, time(2026, 10, 30, 9, 0)));
    }
    @Test public void pastTimeCrossesYear() {
        assertEquals(time(2027, 1, 1, 8, 0),
                ReminderTime.next(8, 0, time(2026, 12, 31, 20, 0)));
    }
    @Test public void takenOnAnotherDateDoesNotSuppressToday() {
        assertFalse(ReminderTime.sameDay(time(2026, 10, 29, 9, 0), time(2026, 10, 30, 9, 0)));
        assertTrue(ReminderTime.sameDay(time(2026, 10, 30, 8, 0), time(2026, 10, 30, 9, 0)));
        assertFalse(ReminderTime.sameDay(0, time(2026, 10, 30, 9, 0)));
    }
    @Test(expected = IllegalArgumentException.class) public void invalidHourRejected() {
        ReminderTime.next(24, 0, System.currentTimeMillis());
    }
}
