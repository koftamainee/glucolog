package com.koftamainee.glucolog.ui.chart

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koftamainee.glucolog.domain.ChartMeal
import com.koftamainee.glucolog.domain.ChartPoint
import com.koftamainee.glucolog.domain.floatToTime
import com.koftamainee.glucolog.ui.theme.ChartBasal
import com.koftamainee.glucolog.ui.theme.ChartBasalDark
import com.koftamainee.glucolog.ui.theme.ChartBolus
import com.koftamainee.glucolog.ui.theme.ChartBolusDark
import com.koftamainee.glucolog.ui.theme.ChartDot
import com.koftamainee.glucolog.ui.theme.ChartGlucoseDark
import com.koftamainee.glucolog.ui.theme.ChartGrid
import com.koftamainee.glucolog.ui.theme.ChartGridDark
import com.koftamainee.glucolog.ui.theme.ChartManual
import com.koftamainee.glucolog.ui.theme.ChartManualDark
import com.koftamainee.glucolog.ui.theme.ChartMeal
import com.koftamainee.glucolog.ui.theme.ChartMealDark
import com.koftamainee.glucolog.ui.theme.ChartRange
import com.koftamainee.glucolog.ui.theme.ChartRangeDark
import com.koftamainee.glucolog.ui.theme.ChartText
import com.koftamainee.glucolog.ui.theme.ChartTextDark
import com.koftamainee.glucolog.ui.theme.GlucologGreen
import kotlin.math.exp

internal const val MIN_G = 0f
internal const val MAX_G = 17.5f
internal const val RANGE_LO = 4f
internal const val RANGE_HI = 8f
internal const val MEAL_G = 14f

internal const val DECAY_K = 0.4
internal const val DECAY_STEP_H = 0.25f
internal const val MAX_DECAY_H = 5f

internal val decayEnd = exp(DECAY_K * MAX_DECAY_H.toDouble())

internal fun decayFactor(dt: Float): Double =
    (exp(DECAY_K * dt.toDouble()) - decayEnd) / (1.0 - decayEnd)

internal data class ChartColors(
    val glucose: Color,
    val glucoseDot: Color,
    val bolus: Color,
    val basal: Color,
    val manual: Color,
    val meal: Color,
    val grid: Color,
    val range: Color,
    val text: Color,
)

@Composable
internal fun chartColors(dark: Boolean): ChartColors = ChartColors(
    glucose = if (dark) ChartGlucoseDark else GlucologGreen,
    glucoseDot = if (dark) ChartGlucoseDark else ChartDot,
    bolus = if (dark) ChartBolusDark else ChartBolus,
    basal = if (dark) ChartBasalDark else ChartBasal,
    manual = if (dark) ChartManualDark else ChartManual,
    meal = if (dark) ChartMealDark else ChartMeal,
    grid = if (dark) ChartGridDark else ChartGrid,
    range = if (dark) ChartRangeDark else ChartRange,
    text = if (dark) ChartTextDark else ChartText,
)

internal fun DrawScope.drawYGrid(
    colors: ChartColors,
    measurer: TextMeasurer,
    toY: (Float) -> Float,
    h: Float,
) {
    for (g in MIN_G.toInt()..MAX_G.toInt()) {
        drawLine(colors.grid, Offset(0f, toY(g.toFloat())), Offset(size.width, toY(g.toFloat())), 0.5.dp.toPx())
    }
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = colors.text,
    )
    for (g in MIN_G.toInt() + 2..MAX_G.toInt() step 2) {
        val text = measurer.measure(AnnotatedString(g.toString()), labelStyle)
        drawText(
            textLayoutResult = text,
            topLeft = Offset(4f, toY(g.toFloat()) - text.size.height / 2f),
        )
    }
}

internal fun DrawScope.drawXGrid(
    colors: ChartColors,
    toX: (Float) -> Float,
    h: Float,
    hours: List<Float>,
) {
    hours.forEach { hour ->
        drawLine(colors.grid, Offset(toX(hour), 0f), Offset(toX(hour), h), 0.5.dp.toPx())
    }
}

internal fun DrawScope.drawRangeBand(
    colors: ChartColors,
    toY: (Float) -> Float,
    w: Float,
) {
    drawRect(
        color = colors.range,
        topLeft = Offset(0f, toY(RANGE_HI)),
        size = Size(w, toY(RANGE_LO) - toY(RANGE_HI)),
    )
}

internal fun DrawScope.drawMarkerLines(
    hours: List<Float>,
    color: Color,
    toX: (Float) -> Float,
    measurer: TextMeasurer,
    usedLabels: MutableList<Rect> = mutableListOf(),
) {
    if (hours.isEmpty()) return
    val labelStyle = TextStyle(
        fontSize = 8.sp,
        fontWeight = FontWeight.Medium,
        color = color,
    )
    val padY = 2.dp.toPx()
    val rowGap = 1.dp.toPx()
    hours.sorted().forEach { hour ->
        val x = toX(hour)
        if (x < 0f || x > size.width) return@forEach
        drawLine(
            color = color.copy(alpha = 0.7f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
        )
        val text = measurer.measure(AnnotatedString(floatToTime(hour % 24f)), labelStyle)
        val left = (x - text.size.width / 2f).coerceIn(0f, size.width - text.size.width)
        var top = padY
        while (usedLabels.any {
                it.overlaps(Rect(left, top, left + text.size.width, top + text.size.height))
            }
        ) {
            top += text.size.height + rowGap
        }
        drawText(
            textLayoutResult = text,
            topLeft = Offset(left, top),
        )
        usedLabels += Rect(left, top, left + text.size.width, top + text.size.height)
    }
}

internal fun DrawScope.drawGlucoseLine(
    line: List<ChartPoint>,
    color: Color,
    toX: (Float) -> Float,
    toY: (Float) -> Float,
) {
    if (line.isEmpty()) return
    val path = Path()
    line.forEachIndexed { i, p ->
        val x = toX(p.h)
        val y = toY(p.g)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
}

internal fun DrawScope.drawManualPoints(
    manual: List<ChartPoint>,
    color: Color,
    measurer: TextMeasurer,
    valueStyle: TextStyle,
    toX: (Float) -> Float,
    toY: (Float) -> Float,
) {
    manual.forEach { p ->
        val x = toX(p.h)
        val y = toY(p.g)
        drawCircle(color, 3.5.dp.toPx(), Offset(x, y))
        val text = measurer.measure(AnnotatedString(fmt(p.g)), valueStyle)
        drawText(
            textLayoutResult = text,
            topLeft = Offset(x + 5.dp.toPx(), y - 2.dp.toPx() - text.size.height),
        )
    }
}

internal fun sampleDecay(start: Float, dose: Float, inWindow: (Float) -> Boolean): List<ChartPoint> {
    val points = mutableListOf<ChartPoint>()
    var dt = 0f
    while (true) {
        // Decay goes from dose down to MIN_G exactly at MAX_DECAY_H, so the
        // curve ends at the chart bottom without a flat clamped segment.
        val value = (MIN_G + (dose - MIN_G) * decayFactor(dt)).toFloat()
        val hour = start + dt
        if (inWindow(hour)) points.add(ChartPoint(hour, value))
        if (dt >= MAX_DECAY_H) break
        dt += DECAY_STEP_H
    }
    return points
}

internal fun DrawScope.drawBolusDecay(
    bolus: List<ChartPoint>,
    prevBolus: List<ChartPoint>,
    color: Color,
    toX: (Float) -> Float,
    toY: (Float) -> Float,
) {
    val curves = dayBolusCurves(bolus, prevBolus)
    curves.forEach { curve ->
        if (curve.size < 2) return@forEach
        val path = Path()
        curve.forEachIndexed { i, p ->
            val x = toX(p.h)
            val y = toY(p.g.coerceIn(MIN_G, MAX_G))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

internal fun dayBolusCurves(
    bolus: List<ChartPoint>,
    prevBolus: List<ChartPoint>,
): List<List<ChartPoint>> {
    val curves = mutableListOf<List<ChartPoint>>()
    bolus.forEach { p -> curves += sampleDecay(p.h, p.g) { it <= 24f } }
    prevBolus.forEach { p -> curves += sampleDecay(p.h - 24f, p.g) { it >= 0f } }
    return curves
}

internal fun DrawScope.drawRoamBolusDecay(
    bolus: List<ChartPoint>,
    color: Color,
    toX: (Float) -> Float,
    toY: (Float) -> Float,
) {
    bolus.forEach { p ->
        val curve = sampleDecay(p.h, p.g) { true }
        if (curve.size < 2) return@forEach
        val path = Path()
        curve.forEachIndexed { i, pt ->
            val x = toX(pt.h)
            val y = toY(pt.g.coerceIn(MIN_G, MAX_G))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

internal fun DrawScope.drawBolusMarkers(
    bolus: List<ChartPoint>,
    color: Color,
    toX: (Float) -> Float,
    toY: (Float) -> Float,
    measurer: TextMeasurer,
    valueStyle: TextStyle,
) {
    bolus.forEach { p ->
        val x = toX(p.h)
        val y = toY(p.g.coerceIn(MIN_G, MAX_G))
        drawCircle(color, 3.5.dp.toPx(), Offset(x, y))
        val text = measurer.measure(AnnotatedString(fmt(p.g)), valueStyle)
        drawText(
            textLayoutResult = text,
            topLeft = Offset(x + 5.dp.toPx(), y - 2.dp.toPx() - text.size.height),
        )
    }
}

internal fun DrawScope.drawBasalMarkers(
    basal: List<ChartPoint>,
    color: Color,
    toX: (Float) -> Float,
    toY: (Float) -> Float,
    measurer: TextMeasurer,
    valueStyle: TextStyle,
) {
    basal.forEach { p ->
        val center = Offset(toX(p.h), toY(MEAL_G))
        drawCircle(color, 3.5.dp.toPx(), center)
        val text = measurer.measure(AnnotatedString(fmt(p.g)), valueStyle)
        drawText(
            textLayoutResult = text,
            topLeft = Offset(center.x + 5.dp.toPx(), center.y - text.size.height / 2f),
        )
    }
}

internal fun DrawScope.drawMealMarkers(
    meals: List<ChartMeal>,
    color: Color,
    toX: (Float) -> Float,
    toY: (Float) -> Float,
    measurer: TextMeasurer,
    carbsStyle: TextStyle,
) {
    meals.forEach { meal ->
        val center = Offset(toX(meal.h), toY(MEAL_G))
        drawCircle(color, 3.5.dp.toPx(), center)
        meal.carbs?.let { c ->
            val text = measurer.measure(AnnotatedString("$c г."), carbsStyle)
            drawText(
                textLayoutResult = text,
                topLeft = Offset(center.x + 6.dp.toPx(), center.y - text.size.height / 2f),
            )
        }
    }
}

internal fun fmt(v: Float): String = String.format(java.util.Locale.ROOT, "%.1f", v)
