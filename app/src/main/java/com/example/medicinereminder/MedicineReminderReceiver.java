package com.example.medicinereminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** 显式用药闹钟，以及开机、时区、系统时间和授权变化。 */
public class MedicineReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        PendingResult result = goAsync();
        Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                if ("com.example.medicinereminder.REMIND".equals(intent.getAction())) {
                    ReminderManager.deliver(app, intent.getLongExtra("medicine_id", -1),
                            intent.getLongExtra("due", 0));
                } else if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())
                        || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                    ReminderManager.timeChanged(app);
                } else {
                    ReminderManager.reconcile(app);
                }
            } catch (Exception e) {
                Log.e("MedicineReminder", "处理提醒失败", e);
            } finally {
                result.finish();
            }
        }, "medicine-reminder").start();
    }
}
