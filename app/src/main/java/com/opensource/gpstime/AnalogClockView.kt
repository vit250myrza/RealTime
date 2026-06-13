package com.opensource.gpstime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.android.material.R
import java.util.Calendar
import java.util.Date

class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val minutePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var accentColor = 0xFF6750A4.toInt()
    private var faceColor = -0x1
    private var rimColor = 0xFFE0E0E0.toInt()
    private var tickColor = 0xFF49454F.toInt()

    private var hours = 0f
    private var minutes = 0f
    private var seconds = 0f

    init {
        tickPaint.style = Paint.Style.STROKE
        tickPaint.strokeCap = Paint.Cap.ROUND
        hourPaint.style = Paint.Style.STROKE
        hourPaint.strokeCap = Paint.Cap.ROUND
        minutePaint.style = Paint.Style.STROKE
        minutePaint.strokeCap = Paint.Cap.ROUND
        secondPaint.style = Paint.Style.STROKE
        secondPaint.strokeCap = Paint.Cap.ROUND
        centerPaint.style = Paint.Style.FILL

        val ta = context.theme.obtainStyledAttributes(intArrayOf(
            com.opensource.gpstime.R.attr.clockAccentColor,
            com.opensource.gpstime.R.attr.clockBackgroundColor,
            com.opensource.gpstime.R.attr.clockTextColor,
            R.attr.colorOutline
        ))
        accentColor = ta.getColor(0, 0xFF6750A4.toInt())
        faceColor = ta.getColor(1, -0x1)
        tickColor = ta.getColor(2, 0xFF49454F.toInt())
        rimColor = ta.getColor(3, 0xFFE0E0E0.toInt())
        ta.recycle()
    }

    fun setAccentColor(color: Int) {
        accentColor = color
        invalidate()
    }

    fun setFaceColor(color: Int) {
        faceColor = color
        invalidate()
    }

    fun setRimColor(color: Int) {
        rimColor = color
        invalidate()
    }

    fun setTime(date: Date) {
        val cal = Calendar.getInstance()
        cal.time = date
        hours = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
        minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
        seconds = cal.get(Calendar.SECOND) + cal.get(Calendar.MILLISECOND) / 1000f
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = Math.min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = Math.min(cx, cy) - 8f

        rimPaint.color = rimColor
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = 4f
        canvas.drawCircle(cx, cy, radius, rimPaint)

        rimPaint.style = Paint.Style.FILL
        rimPaint.color = faceColor
        canvas.drawCircle(cx, cy, radius - 4f, rimPaint)

        tickPaint.strokeWidth = radius * 0.025f
        for (i in 0 until 60) {
            val angle = Math.toRadians((i * 6 - 90).toDouble())
            val tickLen = if (i % 5 == 0) radius * TICK_LONG_RATIO else radius * TICK_SHORT_RATIO
            if (i % 5 == 0) {
                tickPaint.color = accentColor
                tickPaint.strokeWidth = radius * 0.04f
            } else {
                tickPaint.color = tickColor
                tickPaint.strokeWidth = radius * 0.02f
            }
            val inner = radius - tickLen - 8f
            val x1 = cx + inner * Math.cos(angle).toFloat()
            val y1 = cy + inner * Math.sin(angle).toFloat()
            val x2 = cx + (radius - 8f) * Math.cos(angle).toFloat()
            val y2 = cy + (radius - 8f) * Math.sin(angle).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        val hourAngle = Math.toRadians((hours * 30 - 90).toDouble())
        val hourLen = radius * 0.5f
        hourPaint.color = accentColor
        hourPaint.strokeWidth = radius * 0.08f
        canvas.drawLine(cx, cy,
            cx + hourLen * Math.cos(hourAngle).toFloat(),
            cy + hourLen * Math.sin(hourAngle).toFloat(),
            hourPaint)

        val minAngle = Math.toRadians((minutes * 6 - 90).toDouble())
        val minLen = radius * 0.7f
        minutePaint.color = accentColor
        minutePaint.strokeWidth = radius * 0.05f
        canvas.drawLine(cx, cy,
            cx + minLen * Math.cos(minAngle).toFloat(),
            cy + minLen * Math.sin(minAngle).toFloat(),
            minutePaint)

        val secAngle = Math.toRadians((seconds * 6 - 90).toDouble())
        val secLen = radius * 0.75f
        secondPaint.color = accentColor
        secondPaint.strokeWidth = radius * 0.015f
        canvas.drawLine(cx, cy,
            cx + secLen * Math.cos(secAngle).toFloat(),
            cy + secLen * Math.sin(secAngle).toFloat(),
            secondPaint)

        centerPaint.color = accentColor
        canvas.drawCircle(cx, cy, radius * 0.06f, centerPaint)
        centerPaint.color = accentColor
        centerPaint.alpha = 180
        canvas.drawCircle(cx, cy, radius * 0.025f, centerPaint)
    }

    companion object {
        private const val TICK_LONG_RATIO = 0.12f
        private const val TICK_SHORT_RATIO = 0.06f
    }
}
