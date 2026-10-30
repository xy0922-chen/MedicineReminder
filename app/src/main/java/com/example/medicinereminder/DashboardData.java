package com.example.medicinereminder;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 只读看板快照，不更改计划、记录或提醒状态。 */
final class DashboardData {
    long now;
    int[] overview;
    Map<Long, MedicineCardAdapter.Card> plans;
    List<HistoryRecordAdapter.Record> recent;
    final List<String> days = new ArrayList<>();
    final int[] week = new int[7];

    static DashboardData read(MedicineDbHelper helper, long now) {
        return read(helper.getReadableDatabase(), now);
    }

    static DashboardData read(SQLiteDatabase db, long now) {
        DashboardData result = new DashboardData();
        result.now = now;
        result.overview = MedicineHistoryStore.overview(db, now);
        result.plans = MedicineCardAdapter.readCards(db);
        result.recent = HistoryRecordAdapter.read(db, 0, now, 3);
        Map<String, Integer> positions = new HashMap<>();
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(now);
        day.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            String key = HistoryDates.dayKey(day.getTimeInMillis());
            result.days.add(key); positions.put(key, i);
            day.add(Calendar.DAY_OF_YEAR, 1);
        }
        try (Cursor rows = db.query(MedicineHistoryStore.TABLE, new String[]{"taken_at"},
                "taken_at >= ? AND taken_at < ?", new String[]{
                        String.valueOf(HistoryDates.boundary(now, -6)),
                        String.valueOf(HistoryDates.boundary(now, 1))}, null, null, null)) {
            while (rows.moveToNext()) {
                Integer position = positions.get(HistoryDates.dayKey(rows.getLong(0)));
                if (position != null) result.week[position]++;
            }
        }
        return result;
    }
}
