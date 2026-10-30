package com.example.medicinereminder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/** 近七天确认记录数量；不把记录数量解释为服药率。 */
public class HistoryBarChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] counts = new int[7];
    private List<String> dates = new ArrayList<>();
    public HistoryBarChartView(Context c, AttributeSet attrs) { super(c, attrs); }

    void setData(List<String> dates, int[] counts) {
        this.dates = new ArrayList<>(dates);
        this.counts = counts.clone();
        StringBuilder accessible = new StringBuilder(getContext().getString(R.string.board_week_title));
        for (int i = 0; i < dates.size(); i++) {
            accessible.append("，").append(dates.get(i)).append("：").append(counts[i]);
        }
        setContentDescription(accessible.toString());
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (dates.size() != 7) return;
        float dp = getResources().getDisplayMetrics().density;
        float sp = getResources().getDisplayMetrics().scaledDensity;
        float labelSize = 13 * sp;
        float baseline = getHeight() - labelSize * 1.7f;
        float top = labelSize * 1.8f;
        float slot = getWidth() / 7f;
        int maximum = 1;
        for (int count : counts) maximum = Math.max(maximum, count);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getContext().getColor(R.color.app_border));
        c.drawRect(0, baseline, getWidth(), baseline + dp, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 7; i++) {
            float center = slot * (i + .5f), width = Math.min(28 * dp, slot * .55f);
            float height = (baseline - top) * counts[i] / maximum;
            paint.setColor(getContext().getColor(i == 6 ? R.color.brand_primary : R.color.chart_secondary));
            if (height > 0) c.drawRoundRect(center - width / 2, baseline - height,
                    center + width / 2, baseline, 5 * dp, 5 * dp, paint);
            paint.setColor(getContext().getColor(R.color.app_muted));
            paint.setTextSize(labelSize);
            String number = String.valueOf(counts[i]);
            float textWidth = paint.measureText(number);
            if (textWidth > slot - 4 * dp) paint.setTextSize(labelSize * (slot - 4 * dp) / textWidth);
            c.drawText(number, center, baseline - height - 7 * dp, paint);
            paint.setTextSize(labelSize);
            c.drawText(dates.get(i).substring(8), center, getHeight() - 5 * dp, paint);
        }
    }
}
