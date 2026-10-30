package com.example.medicinereminder;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 权限申请集中在新页面，避免改动原有表单和详情交互。 */
public class ReminderSettingsActivity extends AppCompatActivity {
    private TextView status;
    private Button enable, disable;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                refresh();
                Toast.makeText(this, granted ? R.string.reminder_permission_granted
                        : R.string.reminder_permission_denied, Toast.LENGTH_LONG).show();
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reminder_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        status = findViewById(R.id.tvReminderStatus);
        enable = findViewById(R.id.btnEnableReminders);
        disable = findViewById(R.id.btnDisableReminders);
        ReminderManager.createChannel(this);
        findViewById(R.id.btnNotificationPermission).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                openSettings(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
            }
        });
        findViewById(R.id.btnExactPermission).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 31 && !ReminderManager.exactAllowed(this)) {
                openSettings(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            } else {
                Toast.makeText(this, R.string.reminder_exact_ready, Toast.LENGTH_SHORT).show();
            }
        });
        enable.setOnClickListener(v -> {
            if (!ReminderManager.notificationsAllowed(this) || !ReminderManager.exactAllowed(this)) {
                Toast.makeText(this, R.string.reminder_need_permissions, Toast.LENGTH_LONG).show();
                return;
            }
            change(true);
        });
        disable.setOnClickListener(v -> change(false));
        findViewById(R.id.btnReminderBack).setOnClickListener(v -> finish());
    }

    private void openSettings(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.reminder_settings_missing, Toast.LENGTH_LONG).show();
        }
    }

    private void change(boolean value) {
        enable.setEnabled(false);
        disable.setEnabled(false);
        worker.execute(() -> {
            boolean success = true;
            try { ReminderManager.setEnabled(getApplicationContext(), value); }
            catch (Exception e) { success = false; android.util.Log.e("MedicineReminder", "设置失败", e); }
            final boolean ok = success;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                enable.setEnabled(true);
                disable.setEnabled(true);
                refresh();
                Toast.makeText(this, ok ? (value ? R.string.reminder_enabled_message
                        : R.string.reminder_disabled_message) : R.string.reminder_failed,
                        Toast.LENGTH_LONG).show();
            });
        });
    }

    private void refresh() {
        if (status == null) return;
        status.setText(getString(R.string.reminder_status,
                getString(ReminderManager.enabled(this) ? R.string.reminder_on : R.string.reminder_off),
                getString(ReminderManager.notificationsAllowed(this) ? R.string.reminder_allowed : R.string.reminder_blocked),
                getString(ReminderManager.exactAllowed(this) ? R.string.reminder_allowed : R.string.reminder_blocked)));
    }

    @Override protected void onResume() { super.onResume(); refresh(); }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }
}
