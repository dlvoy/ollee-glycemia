package com.arthur.bgollee

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil
import kotlin.math.floor

class GlycemiaGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var displayRangeHours: Int = 4

    private val density = resources.displayMetrics.density
    private val cardRect = RectF()
    private val contentLeft = 40f * density
    private val contentTop = 20f * density
    private val contentRight = 16f * density
    private val contentBottom = 28f * density
    private val dotRadius = 4f * density

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FAFAFA")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D6D6D6")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        pathEffect = DashPathEffect(floatArrayOf(4f * density, 4f * density), 0f)
    }

    private val targetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A0A0A0")
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        pathEffect = DashPathEffect(floatArrayOf(8f * density, 4f * density), 0f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B6B6B")
        textSize = 11f * density
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8A8A8A")
        textSize = 13f * density
        textAlign = Paint.Align.CENTER
    }

    private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }

    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F44336")
        style = Paint.Style.FILL
    }

    private val yellowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFC107")
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) return

        cardRect.set(0f, 0f, width, height)
        canvas.drawRoundRect(cardRect, 18f * density, 18f * density, cardPaint)
        canvas.drawRoundRect(cardRect, 18f * density, 18f * density, borderPaint)

        val entries = GlycemiaHistoryStore.getRecentEntries(context, displayRangeHours * 60L * 60L * 1000L)
        val visibleEntries = entries.filter { it.timestampMs >= currentRangeStart() }
        val (minY, maxY) = computeYRange(visibleEntries)

        val plotLeft = contentLeft
        val plotTop = contentTop
        val plotRight = width - contentRight
        val plotBottom = height - contentBottom

        drawGrid(canvas, plotLeft, plotTop, plotRight, plotBottom, minY, maxY)
        drawLabels(canvas, plotLeft, plotTop, plotRight, plotBottom, minY, maxY)

        if (visibleEntries.isEmpty()) {
            canvas.drawText(
                context.getString(R.string.graph_empty),
                width / 2f,
                height / 2f,
                emptyPaint
            )
            return
        }

        visibleEntries.forEach { entry ->
            val x = mapTimestampToX(entry.timestampMs, plotLeft, plotRight)
            val y = mapValueToY(entry.valueMgDl.toFloat(), minY, maxY, plotTop, plotBottom)
            canvas.drawCircle(x, y, dotRadius, paintForValue(entry.valueMgDl))
        }
    }

    fun setDisplayRange(hours: Int) {
        displayRangeHours = hours.coerceIn(1, 24)
        invalidate()
    }

    fun getDisplayRangeHours(): Int {
        return displayRangeHours
    }

    fun refresh() {
        invalidate()
    }

    private fun computeYRange(entries: List<GlycemiaHistoryEntry>): Pair<Float, Float> {
        if (entries.isEmpty()) return 40f to 200f

        val values = entries.map { it.valueMgDl.toFloat() }
        val minValue = values.minOrNull() ?: 40f
        val maxValue = values.maxOrNull() ?: 200f
        val range = (maxValue - minValue).coerceAtLeast(20f)
        val margin = range * 0.1f
        return (minValue - margin).coerceAtLeast(40f) to (maxValue + margin).coerceAtMost(400f)
    }

    private fun currentRangeStart(): Long {
        return System.currentTimeMillis() - displayRangeHours * 60L * 60L * 1000L
    }

    private fun drawGrid(
        canvas: Canvas,
        plotLeft: Float,
        plotTop: Float,
        plotRight: Float,
        plotBottom: Float,
        minY: Float,
        maxY: Float
    ) {
        val now = System.currentTimeMillis()
        val rangeStart = now - displayRangeHours * 60L * 60L * 1000L
        val verticalStepMs = 15L * 60L * 1000L
        val firstVertical = ((rangeStart + verticalStepMs - 1) / verticalStepMs) * verticalStepMs

        var vertical = firstVertical
        while (vertical <= now) {
            val x = mapTimestampToX(vertical, plotLeft, plotRight)
            canvas.drawLine(x, plotTop, x, plotBottom, gridPaint)
            vertical += verticalStepMs
        }

        val horizontalStart = floor(minY / 10f).toInt() * 10
        val horizontalEnd = ceil(maxY / 10f).toInt() * 10
        for (value in horizontalStart..horizontalEnd step 10) {
            val y = mapValueToY(value.toFloat(), minY, maxY, plotTop, plotBottom)
            val paint = if (value == 70 || value == 180) targetLinePaint else gridPaint
            canvas.drawLine(plotLeft, y, plotRight, y, paint)
        }
    }

    private fun drawLabels(
        canvas: Canvas,
        plotLeft: Float,
        plotTop: Float,
        plotRight: Float,
        plotBottom: Float,
        minY: Float,
        maxY: Float
    ) {
        canvas.drawText(maxY.toInt().toString(), 6f * density, plotTop + 4f * density, labelPaint)
        canvas.drawText(minY.toInt().toString(), 6f * density, plotBottom, labelPaint)

        canvas.drawText(
            context.getString(R.string.graph_now_label),
            plotRight - 20f * density,
            height - 8f * density,
            labelPaint
        )
        canvas.drawText(
            context.getString(R.string.graph_hours_label, displayRangeHours),
            plotLeft,
            height - 8f * density,
            labelPaint
        )
    }

    private fun mapTimestampToX(timestampMs: Long, plotLeft: Float, plotRight: Float): Float {
        val now = System.currentTimeMillis()
        val rangeStart = now - displayRangeHours * 60L * 60L * 1000L
        val fraction = ((timestampMs - rangeStart).toFloat() / (now - rangeStart).toFloat())
            .coerceIn(0f, 1f)
        return plotLeft + (plotRight - plotLeft) * fraction
    }

    private fun mapValueToY(
        value: Float,
        minY: Float,
        maxY: Float,
        plotTop: Float,
        plotBottom: Float
    ): Float {
        val fraction = ((value - minY) / (maxY - minY)).coerceIn(0f, 1f)
        return plotBottom - (plotBottom - plotTop) * fraction
    }

    private fun paintForValue(value: Int): Paint {
        return when {
            value < 70 -> redPaint
            value > 180 -> yellowPaint
            else -> greenPaint
        }
    }
}