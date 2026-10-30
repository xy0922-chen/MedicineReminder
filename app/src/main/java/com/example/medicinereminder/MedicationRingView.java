package com.example.medicinereminder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** 原生 Canvas 环形图，真实数据由外部传入，不引入图表依赖。 */
public class MedicationRingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private int recorded, total;
    public MedicationRingView(Context c, AttributeSet attrs) { super(c, attrs); }

    public void setCounts(int recorded, int total) {
        this.total = Math.max(0, total);
        this.recorded = Math.max(0, Math.min(recorded, this.total));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float stroke = 16 * getResources().getDisplayMetrics().density;
        float diameter = Math.min(getWidth(), getHeight()) - stroke - 8;
        if (diameter <= 0) return;
        float left = (getWidth() - diameter) / 2f, top = (getHeight() - diameter) / 2f;
        bounds.set(left, top, left + diameter, top + diameter);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setColor(getContext().getColor(total == 0 ? R.color.chart_track : R.color.chart_pending));
        canvas.drawArc(bounds, -90, 360, false, paint);
        if (total > 0 && recorded > 0) {
            paint.setColor(getContext().getColor(R.color.chart_recorded));
            canvas.drawArc(bounds, -90, 360f * recorded / total, false, paint);
        }
    }
}
