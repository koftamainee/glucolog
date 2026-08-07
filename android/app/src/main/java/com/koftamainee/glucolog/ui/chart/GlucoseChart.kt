package com.koftamainee.glucolog.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.koftamainee.glucolog.ui.theme.ChartRange
import com.koftamainee.glucolog.ui.theme.ChartRangeDark
import com.koftamainee.glucolog.ui.theme.ChartText
import com.koftamainee.glucolog.ui.theme.ChartTextDark
import com.koftamainee.glucolog.ui.theme.GlucologGreen

private const val MIN_G = 1f
private const val MAX_G = 17.5f
private const val RANGE_LO = 4f
private const val RANGE_HI = 8f
private const val MAX_INS = MAX_G

private data class ChartColors(
    val glucose: Color,
    val glucoseDot: Color,
    val bolus: Color,
    val basal: Color,
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
        grid = if (dark) ChartGridDark else ChartGrid,
        range = if (dark) ChartRangeDark else ChartRange,
        text = if (dark) ChartTextDark else ChartText,
    )
    val measurer = rememberTextMeasurer()
    val chartHeight = 160.dp

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
            drawChart(chart, colors, measurer, crosshair)
        }
    }
}

private fun DrawScope.drawChart(
    chart: ChartModel,
    colors: ChartColors,
    measurer: TextMeasurer,
    crosshair: ChartPoint?,
) {
    val w = size.width
    val h = size.height
    val toX = { hour: Float -> hour / 24f * w }
    val toY = { g: Float -> h - (g - MIN_G) / (MAX_G - MIN_G) * h }
    val toYIns = { v: Float -> h - v / MAX_INS * h }

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

    drawLineSeries(chart.bolus, colors.bolus, colors.bolus, toX, toYIns, measurer, valueStyle)
    drawLineSeries(chart.basal, colors.basal, colors.basal, toX, toYIns, measurer, valueStyle)

    if (chart.line.isNotEmpty()) {
        val path = Path()
        chart.line.forEachIndexed { i, p ->
            val x = toX(p.h)
            val y = toY(p.g)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, colors.glucose, style = Stroke(width = 1.5.dp.toPx()))

        chart.line.forEach { p ->
            drawCircle(colors.glucoseDot, 3.5.dp.toPx(), Offset(toX(p.h), toY(p.g)))
        }
    }

    chart.manual.forEach { p ->
        val x = toX(p.h)
        val y = toY(p.g)
        drawCircle(colors.glucoseDot, 3.5.dp.toPx(), Offset(x, y))
        val text = measurer.measure(AnnotatedString(fmt(p.g)), valueStyle)
        drawText(
            textLayoutResult = text,
            topLeft = Offset(x + 5.dp.toPx(), y - 2.dp.toPx() - text.size.height),
        )
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

private fun DrawScope.drawLineSeries(
    points: List<ChartPoint>,
    color: Color,
    dotColor: Color,
    toX: (Float) -> Float,
    toY: (Float) -> Float,
    measurer: TextMeasurer,
    valueStyle: TextStyle,
) {
    if (points.isEmpty()) return
    if (points.size > 1) {
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = toX(p.h)
            val y = toY(p.g)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
    points.forEach { p ->
        val x = toX(p.h)
        val y = toY(p.g)
        drawCircle(dotColor, 3.5.dp.toPx(), Offset(x, y))
        val text = measurer.measure(AnnotatedString(fmt(p.g)), valueStyle)
        drawText(
            textLayoutResult = text,
            topLeft = Offset(x + 5.dp.toPx(), y - 2.dp.toPx() - text.size.height),
        )
    }
}

private fun fmt(v: Float): String = String.format(java.util.Locale.ROOT, "%.1f", v)
