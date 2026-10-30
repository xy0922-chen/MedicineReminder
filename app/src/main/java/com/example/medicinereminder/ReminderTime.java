package com.example.medicinereminder;

import java.util.Calendar;

/** 时间计算独立出来，方便理解和测试；每天重复按本地日历计算。 */
final class ReminderTime {
    static long next(int hour, int minute, long now) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("无效的计划时间");
        }
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(now);
        day.set(Calendar.HOUR_OF_DAY, hour);
        day.set(Calendar.MINUTE, minute);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        if (day.getTimeInMillis() <= now) day.add(Calendar.DAY_OF_YEAR, 1);
        return day.getTimeInMillis();
    }

    static boolean sameDay(long first, long second) {
        if (first <= 0 || second <= 0) return false;
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        a.setTimeInMillis(first);
        b.setTimeInMillis(second);
        return a.get(Calendar.ERA) == b.get(Calendar.ERA)
                && a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
