package com.opensource.gpstime;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.Date;

public class AnalogClockView extends View {

    private static final float TICK_LONG_RATIO = 0.12f;
    private static final float TICK_SHORT_RATIO = 0.06f;

    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minutePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int accentColor = 0xFF6750A4;
    private int faceColor = 0xFFFFFFFF;
    private int rimColor = 0xFFE0E0E0;
    private int tickColor = 0xFF49454F;

    private float hours = 0;
    private float minutes = 0;
    private float seconds = 0;

    public AnalogClockView(Context context) {
        super(context);
        init();
    }

    public AnalogClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        hourPaint.setStyle(Paint.Style.STROKE);
        hourPaint.setStrokeCap(Paint.Cap.ROUND);

        minutePaint.setStyle(Paint.Style.STROKE);
        minutePaint.setStrokeCap(Paint.Cap.ROUND);

        secondPaint.setStyle(Paint.Style.STROKE);
        secondPaint.setStrokeCap(Paint.Cap.ROUND);

        centerPaint.setStyle(Paint.Style.FILL);

        TypedArray ta = getContext().getTheme().obtainStyledAttributes(new int[]{
                R.attr.clockAccentColor,
                R.attr.clockBackgroundColor,
                R.attr.clockTextColor,
                com.google.android.material.R.attr.colorOutline
        });
        accentColor = ta.getColor(0, 0xFF6750A4);
        faceColor = ta.getColor(1, 0xFFFFFFFF);
        tickColor = ta.getColor(2, 0xFF49454F);
        rimColor = ta.getColor(3, 0xFFE0E0E0);
        ta.recycle();
    }

    public void setAccentColor(int color) {
        accentColor = color;
        invalidate();
    }

    public void setFaceColor(int color) {
        faceColor = color;
        invalidate();
    }

    public void setRimColor(int color) {
        rimColor = color;
        invalidate();
    }

    public void setTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        hours = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f;
        minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f;
        seconds = cal.get(Calendar.SECOND) + cal.get(Calendar.MILLISECOND) / 1000f;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec)
        );
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - 8;

        // Rim
        rimPaint.setColor(rimColor);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(4);
        canvas.drawCircle(cx, cy, radius, rimPaint);

        // Face
        rimPaint.setStyle(Paint.Style.FILL);
        rimPaint.setColor(faceColor);
        canvas.drawCircle(cx, cy, radius - 4, rimPaint);

        // Ticks
        tickPaint.setStrokeWidth(radius * 0.025f);
        for (int i = 0; i < 60; i++) {
            float angle = (float) Math.toRadians(i * 6 - 90);
            float tickLen = (i % 5 == 0) ? radius * TICK_LONG_RATIO : radius * TICK_SHORT_RATIO;
            if (i % 5 == 0) {
                tickPaint.setColor(accentColor);
                tickPaint.setStrokeWidth(radius * 0.04f);
            } else {
                tickPaint.setColor(tickColor);
                tickPaint.setStrokeWidth(radius * 0.02f);
            }
            float inner = radius - tickLen - 8;
            float x1 = cx + inner * (float) Math.cos(angle);
            float y1 = cy + inner * (float) Math.sin(angle);
            float x2 = cx + (radius - 8) * (float) Math.cos(angle);
            float y2 = cy + (radius - 8) * (float) Math.sin(angle);
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        // Hour hand
        float hourAngle = (float) Math.toRadians(hours * 30 - 90);
        float hourLen = radius * 0.5f;
        hourPaint.setColor(accentColor);
        hourPaint.setStrokeWidth(radius * 0.08f);
        canvas.drawLine(cx, cy,
                cx + hourLen * (float) Math.cos(hourAngle),
                cy + hourLen * (float) Math.sin(hourAngle),
                hourPaint);

        // Minute hand
        float minAngle = (float) Math.toRadians(minutes * 6 - 90);
        float minLen = radius * 0.7f;
        minutePaint.setColor(accentColor);
        minutePaint.setStrokeWidth(radius * 0.05f);
        canvas.drawLine(cx, cy,
                cx + minLen * (float) Math.cos(minAngle),
                cy + minLen * (float) Math.sin(minAngle),
                minutePaint);

        // Second hand
        float secAngle = (float) Math.toRadians(seconds * 6 - 90);
        float secLen = radius * 0.75f;
        secondPaint.setColor(accentColor);
        secondPaint.setStrokeWidth(radius * 0.015f);
        canvas.drawLine(cx, cy,
                cx + secLen * (float) Math.cos(secAngle),
                cy + secLen * (float) Math.sin(secAngle),
                secondPaint);

        // Center dot
        centerPaint.setColor(accentColor);
        canvas.drawCircle(cx, cy, radius * 0.06f, centerPaint);
        centerPaint.setColor(accentColor);
        centerPaint.setAlpha(180);
        canvas.drawCircle(cx, cy, radius * 0.025f, centerPaint);
    }
}
