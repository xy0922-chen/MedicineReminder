package com.example.medicinereminder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 只改变历史记录展示；查询范围、200条上限与原版本一致。 */
final class HistoryRecordAdapter extends ArrayAdapter<HistoryRecordAdapter.Record> {
    static final class Record {
        String name, dose, meal;
        long takenAt;
        int hour, minute;
        boolean imported;
    }
    HistoryRecordAdapter(Context context, List<Record> rows) { super(context, 0, rows); }

    static ArrayList<Record> read(SQLiteDatabase db, int days, long now, int limit) {
        String where = days == 0 ? null : "taken_at >= ? AND taken_at < ?";
        String[] args = days == 0 ? null : new String[]{
                String.valueOf(HistoryDates.boundary(now, 1 - days)),
                String.valueOf(HistoryDates.boundary(now, 1))};
        ArrayList<Record> list = new ArrayList<>();
        try (Cursor row = db.query(MedicineHistoryStore.TABLE, null, where, args,
                null, null, "taken_at DESC, _id DESC", String.valueOf(limit))) {
            while (row.moveToNext()) {
                Record r = new Record();
                r.name = row.getString(row.getColumnIndexOrThrow("name"));
                r.dose = row.getString(row.getColumnIndexOrThrow("dose"));
                r.meal = row.getString(row.getColumnIndexOrThrow("meal"));
                r.takenAt = row.getLong(row.getColumnIndexOrThrow("taken_at"));
                r.hour = row.getInt(row.getColumnIndexOrThrow("hour"));
                r.minute = row.getInt(row.getColumnIndexOrThrow("minute"));
                r.imported = row.getInt(row.getColumnIndexOrThrow("imported")) == 1;
                list.add(r);
            }
        }
        return list;
    }

    @Override public View getView(int position, View recycled, ViewGroup parent) {
        View row = recycled == null ? LayoutInflater.from(getContext())
                .inflate(R.layout.item_history_record, parent, false) : recycled;
        bind(getContext(), row, getItem(position));
        return row;
    }

    static void bind(Context c, View row, Record r) {
        ((TextView) row.findViewById(R.id.tvRecordName)).setText(r.name);
        String date = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date(r.takenAt));
        ((TextView) row.findViewById(R.id.tvRecordDate)).setText(date);
        ((TextView) row.findViewById(R.id.tvRecordDose)).setText(c.getString(R.string.board_record_details, r.dose, r.meal));
        ((TextView) row.findViewById(R.id.tvRecordPlan)).setText(c.getString(R.string.board_record_time, r.hour, r.minute));
        row.findViewById(R.id.tvRecordImported).setVisibility(r.imported ? View.VISIBLE : View.GONE);
    }
}
