package com.example.medicinereminder;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 新首页和看板的只读展示协调器，与原增删改查相互独立。 */
final class DashboardController {
    private final AppCompatActivity activity;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private int version;
    private boolean closed;

    DashboardController(AppCompatActivity activity) { this.activity = activity; }

    void refresh() {
        if (closed) return;
        int request = ++version;
        worker.execute(() -> {
            MedicineDbHelper helper = new MedicineDbHelper(activity.getApplicationContext());
            try {
                DashboardData data = DashboardData.read(helper, System.currentTimeMillis());
                activity.runOnUiThread(() -> {
                    if (!closed && request == version && !activity.isFinishing() && !activity.isDestroyed()) show(data);
                });
            } catch (Exception e) {
                Log.e("MedicineDashboard", "读取看板失败", e);
                activity.runOnUiThread(() -> {
                    if (closed || request != version || activity.isFinishing() || activity.isDestroyed()) return;
                    text(R.id.tvDashboardMessage, activity.getString(R.string.board_load_failed));
                    ((LinearLayout) activity.findViewById(R.id.homePendingPlans)).removeAllViews();
                    ((LinearLayout) activity.findViewById(R.id.homeRecentRecords)).removeAllViews();
                    text(R.id.tvHomePendingEmpty, activity.getString(R.string.board_load_failed));
                    text(R.id.tvHomeRecentEmpty, activity.getString(R.string.board_load_failed));
                    activity.findViewById(R.id.tvHomePendingEmpty).setVisibility(View.VISIBLE);
                    activity.findViewById(R.id.tvHomeRecentEmpty).setVisibility(View.VISIBLE);
                    text(R.id.tvHomePlanCount, "—"); text(R.id.tvHomeRecordedCount, "—"); text(R.id.tvHomePendingCount, "—");
                    for (int id : new int[]{R.id.homeRing, R.id.boardRing}) {
                        ((MedicationRingView) activity.findViewById(id)).setCounts(0, 0);
                    }
                    text(R.id.tvHomePercent, "—"); text(R.id.tvBoardPercent, "—");
                    text(R.id.tvHomeChartCaption, activity.getString(R.string.board_load_failed));
                    text(R.id.tvBoardChartCaption, activity.getString(R.string.board_load_failed));
                    for (int id : new int[]{R.id.tvHomeRecordedLegend, R.id.tvHomePendingLegend,
                            R.id.tvBoardRecordedLegend, R.id.tvBoardPendingLegend, R.id.tvWeekTotal}) text(id, "—");
                    ((HistoryBarChartView) activity.findViewById(R.id.weekBarChart)).setData(new ArrayList<>(), new int[7]);
                });
            } finally { helper.close(); }
        });
    }

    private void text(int id, String value) { ((TextView) activity.findViewById(id)).setText(value); }

    private void show(DashboardData d) {
        String date = new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault()).format(new Date(d.now));
        text(R.id.tvHomeDate, date);
        text(R.id.tvBoardDate, date);
        text(R.id.tvHomePlanCount, String.valueOf(d.overview[0]));
        text(R.id.tvHomeRecordedCount, String.valueOf(d.overview[1]));
        text(R.id.tvHomePendingCount, String.valueOf(d.overview[2]));
        updateRing(R.id.homeRing, R.id.tvHomePercent, R.id.tvHomeChartCaption, d.overview);
        updateRing(R.id.boardRing, R.id.tvBoardPercent, R.id.tvBoardChartCaption, d.overview);
        text(R.id.tvHomeRecordedLegend, activity.getString(R.string.board_recorded_value, d.overview[1]));
        text(R.id.tvHomePendingLegend, activity.getString(R.string.board_pending_value, d.overview[2]));
        text(R.id.tvBoardRecordedLegend, activity.getString(R.string.board_recorded_value, d.overview[1]));
        text(R.id.tvBoardPendingLegend, activity.getString(R.string.board_pending_value, d.overview[2]));
        ((HistoryBarChartView) activity.findViewById(R.id.weekBarChart)).setData(d.days, d.week);
        int sum = 0;
        for (int count : d.week) sum += count;
        text(R.id.tvWeekTotal, activity.getString(R.string.board_week_total, sum));
        text(R.id.tvWeekRange, d.days.get(0) + " — " + d.days.get(6));
        text(R.id.tvDashboardMessage, activity.getString(R.string.board_scope));

        LinearLayout pending = activity.findViewById(R.id.homePendingPlans);
        pending.removeAllViews();
        List<MedicineCardAdapter.Card> plans = new ArrayList<>(d.plans.values());
        plans.sort(Comparator.comparingInt((MedicineCardAdapter.Card p) -> p.hour * 60 + p.minute).thenComparingLong(p -> p.id));
        int shown = 0;
        for (MedicineCardAdapter.Card p : plans) {
            if (ReminderTime.sameDay(p.lastTaken, d.now)) continue;
            if (shown++ == 2) break;
            View row = LayoutInflater.from(activity).inflate(R.layout.item_home_plan, pending, false);
            ((TextView) row.findViewById(R.id.tvHomePlanName)).setText(p.name);
            ((TextView) row.findViewById(R.id.tvHomePlanInfo)).setText(activity.getString(
                    R.string.board_plan_info, p.hour, p.minute, p.dose));
            row.setOnClickListener(v -> activity.startActivity(new Intent(activity, MedicineDetailActivity.class)
                    .putExtra("medicine_id", p.id)));
            pending.addView(row);
        }
        TextView pendingEmpty = activity.findViewById(R.id.tvHomePendingEmpty);
        pendingEmpty.setText(d.overview[0] == 0 ? R.string.board_no_plans : R.string.board_all_recorded);
        pendingEmpty.setVisibility(pending.getChildCount() == 0 ? View.VISIBLE : View.GONE);
        LinearLayout recent = activity.findViewById(R.id.homeRecentRecords);
        recent.removeAllViews();
        for (HistoryRecordAdapter.Record record : d.recent) {
            View row = LayoutInflater.from(activity).inflate(R.layout.item_history_record, recent, false);
            HistoryRecordAdapter.bind(activity, row, record);
            recent.addView(row);
        }
        activity.findViewById(R.id.tvHomeRecentEmpty).setVisibility(d.recent.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateRing(int viewId, int percentId, int captionId, int[] overview) {
        ((MedicationRingView) activity.findViewById(viewId)).setCounts(overview[1], overview[0]);
        text(percentId, overview[0] == 0 ? "—" : Math.round(100f * overview[1] / overview[0]) + "%");
        text(captionId, activity.getString(overview[0] == 0 ? R.string.board_no_plan_caption : R.string.board_ratio_caption));
    }

    void close() { closed = true; version++; worker.shutdown(); }
}
