package com.example.medicinereminder;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 按手机本地日期查看历史；已删除计划的历史仍可查。 */
public class MedicineHistoryActivity extends AppCompatActivity {
    private MedicineDbHelper helper;
    private TextView status, empty;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> items = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private RadioGroup range;
    private int requestVersion;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicine_history);
        MedicineBottomNavigation.bindHistory(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        helper = new MedicineDbHelper(this);
        status = findViewById(R.id.tvHistoryCount);
        empty = findViewById(R.id.tvHistoryEmpty);
        range = findViewById(R.id.rgHistoryRange);
        if (state != null) range.check(state.getInt("history_range", R.id.rbHistoryToday));
        ListView list = findViewById(R.id.lvHistory);
        adapter = new ArrayAdapter<>(this, R.layout.item_medicine, items);
        list.setAdapter(adapter);
        list.setEmptyView(empty);
        range.setOnCheckedChangeListener((group, id) -> load());
        findViewById(R.id.btnHistoryBack).setOnClickListener(v -> finish());
    }

    private void load() {
        int id = range.getCheckedRadioButtonId();
        int days = id == R.id.rbHistoryToday ? 1 : id == R.id.rbHistoryWeek ? 7 : 0;
        int version = ++requestVersion;
        items.clear(); adapter.notifyDataSetChanged();
        empty.setText(R.string.history_loading);
        status.setText(R.string.history_loading);
        worker.execute(() -> {
            try {
                ArrayList<String> rows = MedicineHistoryStore.history(
                        helper.getReadableDatabase(), days, System.currentTimeMillis());
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || version != requestVersion) return;
                    items.addAll(rows); adapter.notifyDataSetChanged();
                    empty.setText(R.string.history_empty);
                    status.setText(getString(R.string.history_count, rows.size(), MedicineHistoryStore.DISPLAY_LIMIT));
                });
            } catch (Exception e) {
                Log.e("MedicineHistory", "读取历史失败", e);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || version != requestVersion) return;
                    empty.setText(R.string.history_error); status.setText(R.string.history_error);
                });
            }
        });
    }

    @Override protected void onResume() { super.onResume(); load(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putInt("history_range", range.getCheckedRadioButtonId());
    }
    @Override protected void onDestroy() {
        worker.execute(() -> helper.close()); worker.shutdown(); super.onDestroy();
    }
}
