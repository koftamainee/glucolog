package com.koftamainee.glucolog.ui.chart

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koftamainee.glucolog.data.MarkerLevelSettings
import com.koftamainee.glucolog.data.MarkerLineSettings
import com.koftamainee.glucolog.data.TargetRangeSettings
import com.koftamainee.glucolog.domain.ChartModel
import com.koftamainee.glucolog.domain.ChartPoint
import com.koftamainee.glucolog.domain.floatToTime

@Composable
fun GlucoseChart(
    chart: ChartModel,
    markerLines: MarkerLineSettings,
    markerLevels: MarkerLevelSettings,
    targetRange: TargetRangeSettings,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val colors = chartColors(dark)
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
            drawChart(chart, colors, measurer, markerLines, markerLevels, targetRange, crosshair)
        }
    }
}

private fun DrawScope.drawChart(
    chart: ChartModel,
    colors: ChartColors,
    measurer: TextMeasurer,
    markerLines: MarkerLineSettings,
    markerLevels: MarkerLevelSettings,
    targetRange: TargetRangeSettings,
    crosshair: ChartPoint?,
) {
    val w = size.width
    val h = size.height
    val toX = { hour: Float -> hour / 24f * w }
    val toY = { g: Float -> h - (g - MIN_G) / (MAX_G - MIN_G) * h }

    drawYGrid(colors, measurer, toY, h)
    drawXGrid(colors, toX, h, (0..24 step 6).map { it.toFloat() })
    drawRangeBand(colors, toY, w, targetRange.lo, targetRange.hi)

    val usedLabels = mutableListOf<Rect>()
    if (markerLines.manual) {
        drawMarkerLines(chart.manual.map { it.h }, colors.manual, toX, measurer, usedLabels)
    }
    if (markerLines.meal) {
        drawMarkerLines(chart.meals.map { it.h }, colors.meal, toX, measurer, usedLabels)
    }
    if (markerLines.basal) {
        drawMarkerLines(chart.basal.map { it.h }, colors.basal, toX, measurer, usedLabels)
    }
    if (markerLines.bolusStart) {
        drawMarkerLines(chart.bolus.map { it.h }, colors.bolus, toX, measurer, usedLabels)
    }
    if (markerLines.bolusEnd) {
        drawMarkerLines(
            chart.bolus.map { it.h + MAX_DECAY_H },
            colors.bolus,
            toX,
            measurer,
            usedLabels,
        )
    }

    val valueStyle = TextStyle(fontSize = 9.sp, color = colors.text)

    drawBolusDecay(chart.bolus, chart.prevBolus, colors.bolus, toX, toY)
    drawBolusMarkers(chart.bolus, colors.bolus, toX, toY, measurer, valueStyle)
    drawBasalMarkers(chart.basal, colors.basal, toX, toY, measurer, valueStyle, markerLevels.basal)
    drawGlucoseLine(chart.line, colors.glucose, toX, toY)
    drawManualPoints(chart.manual, colors.manual, measurer, valueStyle, toX, toY)
    drawMealMarkers(
        chart.meals,
        colors.meal,
        toX,
        toY,
        measurer,
        TextStyle(fontSize = 8.sp, color = colors.text),
        markerLevels.meal,
    )

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
