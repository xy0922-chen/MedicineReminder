package com.example.medicinereminder;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** 历史记录独立成表；与原计划的 last_taken_at 在同一事务中更新。 */
final class MedicineHistoryStore {
    static final String TABLE = "medicine_history";
    static final int DISPLAY_LIMIT = 200;

    static void create(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "medicine_id INTEGER NOT NULL,"
                + "day_key TEXT NOT NULL,"
                + "taken_at INTEGER NOT NULL,"
                + "name TEXT NOT NULL, dose TEXT NOT NULL, meal TEXT NOT NULL,"
                + "hour INTEGER NOT NULL, minute INTEGER NOT NULL,"
                + "imported INTEGER NOT NULL DEFAULT 0,"
                + "UNIQUE(medicine_id, day_key))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_time ON " + TABLE + "(taken_at DESC)");
        // 旧版只保存了最近一次时间，因此只能导入这一条，不能恢复更早记录。
        try (Cursor rows = db.query(MedicineDbHelper.TABLE_MEDICINE, null,
                "last_taken_at > 0", null, null, null, null)) {
            while (rows.moveToNext()) {
                long time = rows.getLong(rows.getColumnIndexOrThrow("last_taken_at"));
                ContentValues values = snapshot(rows, time);
                values.put("imported", 1);
                db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }
    }

    private static ContentValues snapshot(Cursor row, long time) {
        ContentValues values = new ContentValues();
        values.put("medicine_id", row.getLong(row.getColumnIndexOrThrow("_id")));
        values.put("day_key", HistoryDates.dayKey(time));
        values.put("taken_at", time);
        for (String column : new String[]{"name", "dose", "meal"}) {
            values.put(column, row.getString(row.getColumnIndexOrThrow(column)));
        }
        values.put("hour", row.getInt(row.getColumnIndexOrThrow("hour")));
        values.put("minute", row.getInt(row.getColumnIndexOrThrow("minute")));
        return values;
    }

    static int mark(SQLiteDatabase db, long id, long now) {
        db.beginTransaction();
        try (Cursor row = db.query(MedicineDbHelper.TABLE_MEDICINE, null, "_id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (!row.moveToFirst()) return 0;
            ContentValues record = snapshot(row, now);
            record.put("imported", 0);
            if (db.insertWithOnConflict(TABLE, null, record, SQLiteDatabase.CONFLICT_REPLACE) == -1) {
                throw new android.database.sqlite.SQLiteException("写入服药历史失败");
            }
            ContentValues latest = new ContentValues();
            latest.put("last_taken_at", now);
            int changed = db.update(MedicineDbHelper.TABLE_MEDICINE, latest, "_id = ?",
                    new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return changed;
        } finally {
            db.endTransaction();
        }
    }

    static int undoToday(SQLiteDatabase db, long id, long now) {
        db.beginTransaction();
        try {
            // 详情页可能持有已被另一页面删除的编号，不能因此清掉保留的历史。
            try (Cursor plan = db.query(MedicineDbHelper.TABLE_MEDICINE, new String[]{"_id"},
                    "_id = ?", new String[]{String.valueOf(id)}, null, null, null)) {
                if (!plan.moveToFirst()) return 0;
            }
            db.delete(TABLE, "medicine_id = ? AND taken_at >= ? AND taken_at < ?",
                    new String[]{String.valueOf(id), String.valueOf(HistoryDates.boundary(now, 0)),
                            String.valueOf(HistoryDates.boundary(now, 1))});
            long latest = 0;
            try (Cursor row = db.rawQuery("SELECT MAX(taken_at) FROM " + TABLE
                    + " WHERE medicine_id = ?", new String[]{String.valueOf(id)})) {
                if (row.moveToFirst() && !row.isNull(0)) latest = row.getLong(0);
            }
            ContentValues values = new ContentValues();
            values.put("last_taken_at", latest);
            int changed = db.update(MedicineDbHelper.TABLE_MEDICINE, values, "_id = ?",
                    new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return changed;
        } finally {
            db.endTransaction();
        }
    }

    /** 统计当前仍存在的所有计划；不是医学依从率，也不是仅统计到期计划。 */
    static int[] overview(SQLiteDatabase db, long now) {
        try (Cursor row = db.rawQuery("SELECT COUNT(*), COALESCE(SUM(CASE WHEN EXISTS ("
                + "SELECT 1 FROM " + TABLE + " h WHERE h.medicine_id = m._id AND h.taken_at >= ? AND h.taken_at < ?"
                + ") THEN 1 ELSE 0 END), 0) FROM medicine m",
                new String[]{String.valueOf(HistoryDates.boundary(now, 0)),
                        String.valueOf(HistoryDates.boundary(now, 1))})) {
            row.moveToFirst();
            int total = row.getInt(0), taken = row.getInt(1);
            return new int[]{total, taken, total - taken};
        }
    }

    static ArrayList<String> history(SQLiteDatabase db, int days, long now) {
        String where = days == 0 ? null : "taken_at >= ? AND taken_at < ?";
        String[] args = days == 0 ? null : new String[]{
                String.valueOf(HistoryDates.boundary(now, 1 - days)),
                String.valueOf(HistoryDates.boundary(now, 1))};
        ArrayList<String> list = new ArrayList<>();
        SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());
        try (Cursor row = db.query(TABLE, null, where, args, null, null,
                "taken_at DESC, _id DESC", String.valueOf(DISPLAY_LIMIT))) {
            while (row.moveToNext()) {
                long time = row.getLong(row.getColumnIndexOrThrow("taken_at"));
                String day = HistoryDates.dayKey(time);
                String name = row.getString(row.getColumnIndexOrThrow("name"));
                String dose = row.getString(row.getColumnIndexOrThrow("dose"));
                String meal = row.getString(row.getColumnIndexOrThrow("meal"));
                int hour = row.getInt(row.getColumnIndexOrThrow("hour"));
                int minute = row.getInt(row.getColumnIndexOrThrow("minute"));
                boolean imported = row.getInt(row.getColumnIndexOrThrow("imported")) == 1;
                list.add(day + "  " + clock.format(new Date(time)) + "  已记录\n"
                        + name + "\n每次用量：" + dose + "；" + meal
                        + String.format(Locale.getDefault(), "\n计划时间：%02d:%02d", hour, minute)
                        + (imported ? "\n旧版最近记录导入，药品信息以升级时为准" : ""));
            }
        }
        return list;
    }
}
