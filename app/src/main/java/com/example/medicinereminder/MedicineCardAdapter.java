package com.example.medicinereminder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 仅负责结构化卡片展示，保留首页原有列表数据与点击编号映射。 */
final class MedicineCardAdapter extends ArrayAdapter<String> {
    static final class Card {
        long id, lastTaken;
        String name, dose, meal;
        int hour, minute;
        boolean daily;
    }

    private Map<Long, Card> cards = new HashMap<>();
    private List<Long> ids = new ArrayList<>();
    private ReminderStatusPresentation reminders;

    MedicineCardAdapter(Context context, List<String> entries) {
        super(context, 0, entries);
    }

    /** 后台一次查询读取字段，避免在主线程逐条查询或拆解显示文字。 */
    static Map<Long, Card> readCards(SQLiteDatabase db) {
        Map<Long, Card> result = new HashMap<>();
        try (Cursor row = db.query(MedicineDbHelper.TABLE_MEDICINE,
                new String[]{"_id", "name", "dose", "meal", "hour", "minute", "repeat_daily", "last_taken_at"},
                null, null, null, null, null)) {
            while (row.moveToNext()) {
                Card card = new Card();
                card.id = row.getLong(0);
                card.name = row.getString(1);
                card.dose = row.getString(2);
                card.meal = row.getString(3);
                card.hour = row.getInt(4);
                card.minute = row.getInt(5);
                card.daily = row.getInt(6) == 1;
                card.lastTaken = row.getLong(7);
                result.put(card.id, card);
            }
        }
        return result;
    }

    void setCards(Map<Long, Card> snapshot, List<Long> orderedIds) {
        cards = new HashMap<>(snapshot);
        ids = new ArrayList<>(orderedIds);
    }

    void setReminderStatus(ReminderStatusPresentation state) {
        reminders = state;
        notifyDataSetChanged();
    }

    @Override public View getView(int position, View reusable, ViewGroup parent) {
        Holder holder;
        if (reusable == null) {
            reusable = LayoutInflater.from(getContext()).inflate(R.layout.item_medicine_card, parent, false);
            holder = new Holder(reusable);
            reusable.setTag(holder);
        } else {
            holder = (Holder) reusable.getTag();
        }
        Card card = position < ids.size() ? cards.get(ids.get(position)) : null;
        if (card == null) {
            holder.name.setText(R.string.card_changed);
            holder.details.setVisibility(View.GONE);
            return reusable;
        }
        holder.details.setVisibility(View.VISIBLE);
        holder.name.setText(card.name);
        holder.time.setText(getContext().getString(R.string.card_time, card.hour, card.minute));
        holder.dose.setText(getContext().getString(R.string.card_dose, card.dose));
        holder.meal.setText(getContext().getString(R.string.card_meal, card.meal));
        holder.repeat.setText(card.daily ? R.string.card_daily : R.string.card_once);
        boolean taken = MedicineDbHelper.isTakenToday(card.lastTaken);
        holder.status.setText(MedicineDbHelper.getTakenStatus(card.lastTaken));
        holder.status.setBackgroundResource(taken ? R.drawable.bg_status_green : R.drawable.bg_status_neutral);
        holder.status.setTextColor(android.graphics.Color.parseColor(taken ? "#174D39" : "#56665D"));
        holder.reminder.setText(reminders == null ? getContext().getString(R.string.reminder_summary_unavailable)
                : reminders.planText(getContext(), card.id, card.daily));
        return reusable;
    }

    private static final class Holder {
        final TextView name, time, dose, meal, repeat, status, reminder;
        final View details;
        Holder(View row) {
            name = row.findViewById(R.id.tvCardName);
            time = row.findViewById(R.id.tvCardTime);
            dose = row.findViewById(R.id.tvCardDose);
            meal = row.findViewById(R.id.tvCardMeal);
            repeat = row.findViewById(R.id.tvCardRepeat);
            status = row.findViewById(R.id.tvCardTaken);
            reminder = row.findViewById(R.id.tvCardReminder);
            details = row.findViewById(R.id.cardDetails);
        }
    }
}
