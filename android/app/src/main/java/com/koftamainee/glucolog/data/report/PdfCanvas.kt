package com.koftamainee.glucolog.data.report

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface

object PdfColors {
    const val TEXT = 0xFF1A1A18.toInt()
    const val TEXT_SEC = 0xFF6B6B67.toInt()
    const val TEXT_TER = 0xFF9E9E99.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()

    const val GREEN = 0xFF1D9E75.toInt()
    const val BOLUS = 0xFFE05A33.toInt()
    const val BASAL = 0xFF507FCC.toInt()

    const val GRID = 0x22000000
    const val SURFACE = 0xFFF5F5F4.toInt()
    const val SURFACE_DARK = 0xFFEEEDE9.toInt()

    const val VERY_LOW = 0xFFC62828.toInt()
    const val LOW = 0xFFE05A33.toInt()
    const val TARGET = 0xFF1D9E75.toInt()
    const val HIGH = 0xFFE8A33D.toInt()
    const val VERY_HIGH = 0xFF7B1FA2.toInt()

    const val WARN_BG = 0xFFFAEEDA.toInt()
    const val WARN_TEXT = 0xFF633806.toInt()
    const val INFO_BG = 0xFFE6F1FB.toInt()
    const val INFO_TEXT = 0xFF0C447C.toInt()

    fun band(kind: com.koftamainee.glucolog.domain.RangeBandKind): Int = when (kind) {
        com.koftamainee.glucolog.domain.RangeBandKind.VERY_LOW -> VERY_LOW
        com.koftamainee.glucolog.domain.RangeBandKind.LOW -> LOW
        com.koftamainee.glucolog.domain.RangeBandKind.TARGET -> TARGET
        com.koftamainee.glucolog.domain.RangeBandKind.HIGH -> HIGH
        com.koftamainee.glucolog.domain.RangeBandKind.VERY_HIGH -> VERY_HIGH
    }
}

enum class PdfAlign { LEFT, CENTER, RIGHT }

private const val ELLIPSIS = "…"

class PdfCanvas(val canvas: Canvas) {

    private val regularFace = Typeface.create("sans-serif", Typeface.NORMAL)
    private val boldFace = Typeface.create("sans-serif", Typeface.BOLD)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    val lineHeight: Float get() = textPaint.textSize * 1.35f

    fun textWidth(value: String, size: Float, bold: Boolean = false): Float {
        textPaint.textSize = size
        textPaint.typeface = if (bold) boldFace else regularFace
        return textPaint.measureText(value)
    }

    fun textHeight(size: Float): Float = size * 1.35f

    fun text(
        value: String,
        x: Float,
        y: Float,
        size: Float = 9f,
        color: Int = PdfColors.TEXT,
        bold: Boolean = false,
        align: PdfAlign = PdfAlign.LEFT,
        maxWidth: Float? = null,
    ) {
        val content = if (maxWidth != null) ellipsize(value, maxWidth, size, bold) else value
        textPaint.textSize = size
        textPaint.color = color
        textPaint.typeface = if (bold) boldFace else regularFace
        val w = textPaint.measureText(content)
        val left = when (align) {
            PdfAlign.LEFT -> x
            PdfAlign.CENTER -> x - w / 2f
            PdfAlign.RIGHT -> x - w
        }
        canvas.drawText(content, left, y, textPaint)
    }

    fun ellipsize(value: String, maxWidth: Float, size: Float, bold: Boolean = false): String {
        if (maxWidth <= 0f) return ""
        if (textWidth(value, size, bold) <= maxWidth) return value
        val ell = textWidth(ELLIPSIS, size, bold)
        var cut = value.length
        while (cut > 0 && textWidth(value.substring(0, cut), size, bold) + ell > maxWidth) cut--
        return if (cut <= 0) "" else value.substring(0, cut).trimEnd() + ELLIPSIS
    }

    fun wrap(value: String, maxWidth: Float, size: Float, bold: Boolean = false): List<String> {
        val out = mutableListOf<String>()
        value.split('\n').forEach { paragraph ->
            var current = StringBuilder()
            paragraph.trim().split(' ').filter { it.isNotEmpty() }.forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (textWidth(candidate, size, bold) <= maxWidth) {
                    current = StringBuilder(candidate)
                } else {
                    if (current.isNotEmpty()) out += current.toString()
                    current = StringBuilder(word)
                }
            }
            if (current.isNotEmpty()) out += current.toString()
        }
        return out
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float = 0.7f) {
        strokePaint.pathEffect = null
        strokePaint.color = color
        strokePaint.strokeWidth = width
        strokePaint.strokeCap = Paint.Cap.BUTT
        canvas.drawLine(x1, y1, x2, y2, strokePaint)
    }

    fun dashedLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float = 0.7f) {
        dashPaint.color = color
        dashPaint.strokeWidth = width
        dashPaint.strokeCap = Paint.Cap.BUTT
        canvas.drawLine(x1, y1, x2, y2, dashPaint)
    }

    fun fillRect(left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        fillPaint.color = color
        fillPaint.style = Paint.Style.FILL
        canvas.drawRect(left, top, right, bottom, fillPaint)
    }

    fun strokeRect(left: Float, top: Float, right: Float, bottom: Float, color: Int, width: Float = 0.7f) {
        strokePaint.pathEffect = null
        strokePaint.color = color
        strokePaint.strokeWidth = width
        canvas.drawRect(left, top, right, bottom, strokePaint)
    }

    fun circle(cx: Float, cy: Float, r: Float, fillColor: Int?, strokeColor: Int?, width: Float = 0.7f) {
        fillColor?.let {
            fillPaint.color = it
            fillPaint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, r, fillPaint)
        }
        strokeColor?.let {
            strokePaint.pathEffect = null
            strokePaint.color = it
            strokePaint.strokeWidth = width
            canvas.drawCircle(cx, cy, r, strokePaint)
        }
    }

    fun polyline(
        points: List<Pair<Float, Float>>,
        strokeColor: Int?,
        width: Float = 0.8f,
        fillColor: Int? = null,
        close: Boolean = false,
    ) {
        if (points.size < 2) return
        val path = Path()
        path.moveTo(points[0].first, points[0].second)
        for (i in 1 until points.size) path.lineTo(points[i].first, points[i].second)
        if (close) path.close()

        fillColor?.let {
            fillPaint.color = it
            fillPaint.style = Paint.Style.FILL
            canvas.drawPath(path, fillPaint)
        }
        strokeColor?.let {
            strokePaint.pathEffect = null
            strokePaint.color = it
            strokePaint.strokeWidth = width
            strokePaint.strokeCap = Paint.Cap.ROUND
            strokePaint.strokeJoin = Paint.Join.ROUND
            canvas.drawPath(path, strokePaint)
        }
    }
}
