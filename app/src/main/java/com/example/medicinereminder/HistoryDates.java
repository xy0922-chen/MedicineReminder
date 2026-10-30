package com.example.medicinereminder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 午夜按本地日历计算，兼容跨月、跨年和夏令时。 */
final class HistoryDates {
    static String dayKey(long time) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(time));
    }

    static String firstDay(long now, int days) {
        if (days < 1) throw new IllegalArgumentException("days must be positive");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.add(Calendar.DAY_OF_YEAR, 1 - days);
        return dayKey(c.getTimeInMillis());
    }

    static long boundary(long now, int dayOffset) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, dayOffset);
        return c.getTimeInMillis();
    }
}
