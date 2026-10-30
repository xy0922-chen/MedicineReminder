package com.example.medicinereminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/** 独立提醒模块。只读取原数据库，不改变原表结构和已有的查询方法。 */
public final class ReminderManager {
    private static final String CHANNEL = "medicine_reminders_v1";
    private static final String PREFIX = "due_";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("medicine_reminders", Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context c) {
        return prefs(c).getBoolean("enabled", false);
    }

    public static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "用药提醒", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("在计划时间显示药品、用量和服用备注");
            c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    public static boolean notificationsAllowed(Context c) {
        createChannel(c);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        if (!NotificationManagerCompat.from(c).areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = c.getSystemService(NotificationManager.class)
                    .getNotificationChannel(CHANNEL);
            return channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
        }
        return true;
    }

    public static boolean exactAllowed(Context c) {
        AlarmManager manager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms();
    }

    /** 从设置页后台调用；关闭后保留计划，移除闹钟和已经显示的通知。 */
    public static synchronized void setEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("enabled", value).commit();
        if (value) {
            reconcile(c);
        } else {
            for (String key : prefs(c).getAll().keySet()) {
                if (key.startsWith(PREFIX)) cancel(c, Long.parseLong(key.substring(PREFIX.length())));
            }
        }
    }

    /** 插入或修改计划后调用；旧闹钟由相同编号的新闹钟替换。 */
    public static synchronized void changed(Context c, long id) {
        try {
            cancel(c, id);
            prefs(c).edit().remove(PREFIX + id).commit();
            if (enabled(c)) reconcile(c);
        } catch (Exception e) {
            // 提醒失败不能把已经成功的数据库保存误报为失败。
            Log.e("MedicineReminder", "更新提醒失败，请到提醒设置检查权限", e);
        }
    }

    public static synchronized void removed(Context c, long id) {
        try {
            cancel(c, id);
            prefs(c).edit().remove(PREFIX + id).commit();
        } catch (Exception e) {
            Log.e("MedicineReminder", "取消提醒失败", e);
        }
    }

    public static synchronized void taken(Context c, long id) {
        NotificationManagerCompat.from(c).cancel("medicine_" + id, 1);
        // 保留未来闹钟；触发时按计划所在日期检查是否已服药。
    }

    /** 首页恢复、开机、时间变化和重新授权后调用。0 表示仅一次提醒已经处理。 */
    public static synchronized void reconcile(Context c) {
        if (!enabled(c) || !exactAllowed(c)) return;
        MedicineDbHelper helper = new MedicineDbHelper(c);
        try (Cursor rows = helper.getReadableDatabase().query(MedicineDbHelper.TABLE_MEDICINE,
                new String[]{"_id", "hour", "minute", "repeat_daily"},
                null, null, null, null, null)) {
            while (rows.moveToNext()) {
                long id = rows.getLong(0);
                String key = PREFIX + id;
                long due = prefs(c).getLong(key, -1);
                if (due == -1 || (due == 0 && rows.getInt(3) == 1)) {
                    due = ReminderTime.next(rows.getInt(1), rows.getInt(2), System.currentTimeMillis());
                    prefs(c).edit().putLong(key, due).commit();
                }
                if (due > 0) schedule(c, id, due);
            }
        } finally {
            helper.close();
        }
    }

    /** 用户改变本地时间或时区后，尚未结束的计划重新按本地时分安排。 */
    static synchronized void timeChanged(Context c) {
        for (String key : prefs(c).getAll().keySet()) {
            if (key.startsWith(PREFIX) && prefs(c).getLong(key, 0) > 0) {
                long id = Long.parseLong(key.substring(PREFIX.length()));
                cancel(c, id);
                prefs(c).edit().remove(key).commit();
            }
        }
        reconcile(c);
    }

    private static Intent alarmIntent(Context c, long id) {
        return new Intent(c, MedicineReminderReceiver.class)
                .setAction("com.example.medicinereminder.REMIND")
                .setData(Uri.parse("medicine-reminder://plan/" + id))
                .putExtra("medicine_id", id);
    }

    private static void schedule(Context c, long id, long due) {
        PendingIntent operation = PendingIntent.getBroadcast(c, 0,
                alarmIntent(c, id).putExtra("due", due),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager manager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    Math.max(due, System.currentTimeMillis() + 1000), operation);
        } catch (SecurityException e) {
            Log.w("MedicineReminder", "精确提醒权限尚未开启", e);
        }
    }

    private static void cancel(Context c, long id) {
        PendingIntent operation = PendingIntent.getBroadcast(c, 0, alarmIntent(c, id),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (operation != null) {
            ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(operation);
            operation.cancel();
        }
        NotificationManagerCompat.from(c).cancel("medicine_" + id, 1);
    }

    static synchronized void deliver(Context c, long id, long due) {
        if (!enabled(c) || due <= 0 || prefs(c).getLong(PREFIX + id, 0) != due) return;
        MedicineDbHelper helper = new MedicineDbHelper(c);
        try (Cursor row = helper.getMedicineById(id)) {
            if (!row.moveToFirst()) { removed(c, id); return; }
            int hour = row.getInt(row.getColumnIndexOrThrow("hour"));
            int minute = row.getInt(row.getColumnIndexOrThrow("minute"));
            boolean daily = row.getInt(row.getColumnIndexOrThrow("repeat_daily")) == 1;
            long taken = row.getLong(row.getColumnIndexOrThrow("last_taken_at"));
            long now = System.currentTimeMillis();
            long next = daily ? ReminderTime.next(hour, minute, now) : 0;
            // 先持久化处理状态，防止重启或重复广播导致仅一次计划重复提醒。
            prefs(c).edit().putLong(PREFIX + id, next).commit();
            if (next > 0) schedule(c, id, next);
            // 不补发昨天等过期日期的通知，避免造成错误服药提示。
            if (!ReminderTime.sameDay(due, now) || ReminderTime.sameDay(taken, due)) return;
            String name = row.getString(row.getColumnIndexOrThrow("name"));
            String dose = row.getString(row.getColumnIndexOrThrow("dose"));
            String meal = row.getString(row.getColumnIndexOrThrow("meal"));
            show(c, id, name, dose, meal);
        } finally {
            helper.close();
        }
    }

    private static void show(Context c, long id, String name, String dose, String meal) {
        if (!notificationsAllowed(c)) return;
        Intent detail = new Intent(c, MedicineDetailActivity.class)
                .setData(Uri.parse("medicine-reminder://detail/" + id))
                .putExtra("medicine_id", id);
        PendingIntent click = PendingIntent.getActivity(c, 0, detail,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String text = c.getString(R.string.reminder_message, name, dose, meal);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_medicine_reminder)
                .setContentTitle(c.getString(R.string.reminder_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(click).setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);
        try {
            NotificationManagerCompat.from(c).notify("medicine_" + id, 1, notification.build());
        } catch (SecurityException e) {
            Log.w("MedicineReminder", "通知权限已关闭", e);
        }
    }
}
