package com.koftamainee.glucolog.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.koftamainee.glucolog.domain.ChartModel
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

private const val MIN_G = 1f
private const val MAX_G = 17.5f
private const val RANGE_LO = 4f
private const val RANGE_HI = 8f
private const val MEAL_G = 12f

private const val DECAY_K = 0.4
private const val DECAY_STEP_H = 0.25f
private const val MAX_DECAY_H = 5f

private val decayEnd = exp(DECAY_K * MAX_DECAY_H.toDouble())

private fun decayFactor(dt: Float): Double =
    (exp(DECAY_K * dt.toDouble()) - decayEnd) / (1.0 - decayEnd)

private data class ChartColors(
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
fun GlucoseChart(
    chart: ChartModel,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val colors = ChartColors(
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
    val measurer = rememberTextMeasurer()
    val chartHeight = 160.dp
    val mealIcon = remember(Icons.Filled.RestaurantMenu) { iconPath(Icons.Filled.RestaurantMenu) }

    var crosshair by remember { mutableStateOf<ChartPoint?>(null) }

    val allGlucose = remember(chart) {
        (chart.line + chart.manual).sortedBy { it.h }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(chart) {
                awaitEachGesture {
                    awaitFirstDown()
                    var target: ChartPoint? = null
                    do {
                        val event = awaitPointerEvent()
                        val pos = event.changes.first().position
                        val targetH = pos.x / size.width * 24f
                        target = allGlucose.minByOrNull { kotlin.math.abs(it.h - targetH) }
                        crosshair = target
                    } while (event.changes.any { it.pressed })
                    crosshair = null
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            drawChart(chart, colors, measurer, crosshair, mealIcon)
        }
    }
}

private fun DrawScope.drawChart(
    chart: ChartModel,
    colors: ChartColors,
    measurer: TextMeasurer,
    crosshair: ChartPoint?,
    mealIcon: Path,
) {
    val w = size.width
    val h = size.height
    val toX = { hour: Float -> hour / 24f * w }
    val toY = { g: Float -> h - (g - MIN_G) / (MAX_G - MIN_G) * h }

    for (g in MIN_G.toInt()..MAX_G.toInt()) {
        drawLine(colors.grid, Offset(0f, toY(g.toFloat())), Offset(w, toY(g.toFloat())), 0.5.dp.toPx())
    }
    for (hour in 0..24 step 6) {
        drawLine(colors.grid, Offset(toX(hour.toFloat()), 0f), Offset(toX(hour.toFloat()), h), 0.5.dp.toPx())
    }

    val labelStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = colors.text,
    )
    for (g in MIN_G.toInt() + 1..MAX_G.toInt() step 2) {
        val text = measurer.measure(AnnotatedString(g.toString()), labelStyle)
        drawText(
            textLayoutResult = text,
            topLeft = Offset(4f, toY(g.toFloat()) - text.size.height / 2f),
        )
    }

    drawRect(
        color = colors.range,
        topLeft = Offset(0f, toY(RANGE_HI)),
        size = Size(w, toY(RANGE_LO) - toY(RANGE_HI)),
    )

    val valueStyle = TextStyle(fontSize = 9.sp, color = colors.text)

    drawBolusDecay(chart.bolus, chart.prevBolus, colors.bolus, toX, toY)
    drawBolusMarkers(chart.bolus, colors.bolus, toX, toY, measurer, valueStyle)
    drawBasalMarkers(chart.basal, colors.basal, toX, toY, measurer, valueStyle)

    if (chart.line.isNotEmpty()) {
        val path = Path()
        chart.line.forEachIndexed { i, p ->
            val x = toX(p.h)
            val y = toY(p.g)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, colors.glucose, style = Stroke(width = 1.5.dp.toPx()))
    }

    chart.manual.forEach { p ->
        val x = toX(p.h)
        val y = toY(p.g)
        drawCircle(colors.manual, 3.5.dp.toPx(), Offset(x, y))
        val text = measurer.measure(AnnotatedString(fmt(p.g)), valueStyle)
        drawText(
            textLayoutResult = text,
            topLeft = Offset(x + 5.dp.toPx(), y - 2.dp.toPx() - text.size.height),
        )
    }

    chart.meals.forEach { p ->
        // Temporary hack: meal icons render ~6 hours earlier than the DB time.
        // Shift them forward on the chart only; DB is not modified.
        val x = toX(p.h + 6f) - 1.dp.toPx()
        drawIcon(mealIcon, colors.meal, 13.dp.toPx(), Offset(x, toY(MEAL_G)))
    }

    crosshair?.let { p ->
        val x = toX(p.h)
        drawLine(
            color = colors.text.copy(alpha = 0.4f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1.dp.toPx(),
        )
        val label = "${floatToTime(p.h)} · ${fmt(p.g)}"
        val text = measurer.measure(
            AnnotatedString(label),
            TextStyle(fontSize = 11.sp, color = colors.text),
        )
        val padX = 6.dp.toPx()
        val padY = 4.dp.toPx()
        val bgW = text.size.width + padX * 2
        val bgH = text.size.height + padY * 2
        val bgX = (x - bgW / 2f).coerceIn(0f, w - bgW)
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.7f),
            topLeft = Offset(bgX, 4.dp.toPx()),
            size = Size(bgW, bgH),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )
        drawText(
            textLayoutResult = text,
            topLeft = Offset(bgX + padX, 4.dp.toPx() + padY),
        )
    }
}

private fun DrawScope.drawBolusDecay(
    bolus: List<ChartPoint>,
    prevBolus: List<ChartPoint>,
    color: Color,
    toX: (Float) -> Float,
    toY: (Float) -> Float,
) {
    val curves = bolusDecayCurves(bolus, prevBolus)
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

private fun bolusDecayCurves(
    bolus: List<ChartPoint>,
    prevBolus: List<ChartPoint>,
): List<List<ChartPoint>> {
    fun sample(start: Float, dose: Float, inWindow: (Float) -> Boolean): List<ChartPoint> {
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
    val curves = mutableListOf<List<ChartPoint>>()
    bolus.forEach { p -> curves += sample(p.h, p.g) { it <= 24f } }
    prevBolus.forEach { p -> curves += sample(p.h - 24f, p.g) { it >= 0f } }
    return curves
}

private fun DrawScope.drawBolusMarkers(
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

private fun DrawScope.drawBasalMarkers(
    basal: List<ChartPoint>,
    color: Color,
    toX: (Float) -> Float,
    toY: (Float) -> Float,
    measurer: TextMeasurer,
    valueStyle: TextStyle,
) {
    basal.forEach { p ->
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

private const val ICON_VIEWPORT = 24f

private fun iconPath(image: ImageVector): Path {
    val path = Path()
    fun append(node: VectorNode) {
        when (node) {
            is VectorPath -> path.addPath(node.pathData.toPath())
            is VectorGroup -> node.forEach { append(it) }
            else -> Unit
        }
    }
    append(image.root)
    return path
}

private fun DrawScope.drawIcon(
    path: Path,
    color: Color,
    size: Float,
    center: Offset,
) {
    withTransform({
        translate(center.x - size / 2, center.y - size / 2)
        scale(size / ICON_VIEWPORT, size / ICON_VIEWPORT)
    }) {
        drawPath(path, color)
    }
}

private fun fmt(v: Float): String = String.format(java.util.Locale.ROOT, "%.1f", v)
