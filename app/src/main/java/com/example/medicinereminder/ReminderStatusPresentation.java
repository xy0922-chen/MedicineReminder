package com.example.medicinereminder;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 只读取提醒开关、权限与已保存安排，绝不创建/取消任务。 */
final class ReminderStatusPresentation {
    final boolean enabled, notifications, exact;
    final Map<Long, Long> schedules;

    ReminderStatusPresentation(boolean enabled, boolean notifications, boolean exact,
                               Map<Long, Long> schedules) {
        this.enabled = enabled;
        this.notifications = notifications;
        this.exact = exact;
        this.schedules = new HashMap<>(schedules);
    }

    static ReminderStatusPresentation read(Context c) {
        return new ReminderStatusPresentation(ReminderManager.enabled(c),
                ReminderManager.notificationsAllowed(c), ReminderManager.exactAllowed(c),
                ReminderManager.savedSchedules(c));
    }

    boolean ready() { return enabled && notifications && exact; }

    int title() {
        if (!enabled) return R.string.reminder_summary_off;
        if (!notifications && !exact) return R.string.reminder_summary_both_blocked;
        if (!notifications) return R.string.reminder_summary_notification_blocked;
        if (!exact) return R.string.reminder_summary_exact_blocked;
        return R.string.reminder_summary_ready;
    }

    String description(Context c) {
        if (!enabled) return c.getString(R.string.reminder_summary_off_help);
        if (!notifications || !exact) return c.getString(R.string.reminder_summary_blocked_help);
        int pending = 0;
        for (long due : schedules.values()) if (due > 0) pending++;
        return pending == 0 ? c.getString(R.string.reminder_summary_no_pending)
                : c.getString(R.string.reminder_summary_pending, pending);
    }

    String planText(Context c, long id, boolean daily) {
        if (!ready()) return c.getString(title());
        Long due = schedules.get(id);
        if (due == null) return c.getString(R.string.card_no_schedule);
        if (due == 0) return c.getString(daily ? R.string.card_no_schedule : R.string.card_once_finished);
        if (due <= System.currentTimeMillis()) return c.getString(R.string.card_schedule_pending);
        String time = new SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(new Date(due));
        return c.getString(R.string.card_next_schedule, time);
    }
}
