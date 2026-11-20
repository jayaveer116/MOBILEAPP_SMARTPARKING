package com.example.project.ui.prediction

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.example.project.data.model.HourlyPrediction

class CustomPredictionChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var predictions: List<HourlyPrediction> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A73E8")
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5F6368")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val peakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D93025") // Material Red for >80% Peak
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    fun setPredictionData(data: List<HourlyPrediction>) {
        this.predictions = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (predictions.isEmpty()) return

        val paddingLeft = 60f
        val paddingRight = 40f
        val paddingTop = 60f
        val paddingBottom = 70f

        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        if (graphWidth <= 0 || graphHeight <= 0) return

        // 1. Draw horizontal grid lines (0%, 50%, 80% Peak, 100%)
        val levels = listOf(0, 50, 80, 100)
        for (lvl in levels) {
            val y = paddingTop + graphHeight * (1f - (lvl / 100f))
            if (lvl == 80) {
                canvas.drawLine(paddingLeft, y, width - paddingRight, y, peakPaint)
                canvas.drawText("Peak Threshold (80%)", width - paddingRight - 120f, y - 10f, textPaint)
            } else {
                canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
            }
            canvas.drawText("$lvl%", paddingLeft - 30f, y + 10f, textPaint)
        }

        // 2. Build curve path
        val path = Path()
        val fillPath = Path()

        val stepX = graphWidth / (predictions.size - 1).coerceAtLeast(1)

        val points = predictions.mapIndexed { index, p ->
            val x = paddingLeft + (index * stepX)
            val y = paddingTop + graphHeight * (1f - (p.predictedOccupancyPercent / 100f))
            Pair(x, y)
        }

        if (points.isNotEmpty()) {
            path.moveTo(points[0].first, points[0].second)
            fillPath.moveTo(points[0].first, paddingTop + graphHeight)
            fillPath.lineTo(points[0].first, points[0].second)

            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val cx = (p1.first + p2.first) / 2f
                path.cubicTo(cx, p1.second, cx, p2.second, p2.first, p2.second)
                fillPath.cubicTo(cx, p1.second, cx, p2.second, p2.first, p2.second)
            }

            fillPath.lineTo(points.last().first, paddingTop + graphHeight)
            fillPath.close()
        }

        // 3. Draw gradient fill
        fillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + graphHeight,
            Color.parseColor("#4285F4"), Color.parseColor("#00FFFFFF"),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        // 4. Draw main prediction trend line
        canvas.drawPath(path, linePaint)

        // 5. Draw X-Axis Hour Labels (Every 4 hours: 00, 04, 08, 12, 16, 20, 23)
        predictions.forEachIndexed { index, p ->
            if (p.hourOfDay % 4 == 0 || index == predictions.size - 1) {
                val x = paddingLeft + (index * stepX)
                val hourLabel = String.format("%02d:00", p.hourOfDay)
                canvas.drawText(hourLabel, x, height - 15f, textPaint)
            }
        }
    }
}
