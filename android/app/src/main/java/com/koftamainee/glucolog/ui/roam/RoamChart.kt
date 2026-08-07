package com.koftamainee.glucolog.ui.roam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koftamainee.glucolog.domain.RoamModel
import com.koftamainee.glucolog.ui.chart.MIN_G
import com.koftamainee.glucolog.ui.chart.MAX_G
import com.koftamainee.glucolog.ui.chart.chartColors
import com.koftamainee.glucolog.ui.chart.drawBasalMarkers
import com.koftamainee.glucolog.ui.chart.drawBolusMarkers
import com.koftamainee.glucolog.ui.chart.drawGlucoseLine
import com.koftamainee.glucolog.ui.chart.drawManualPoints
import com.koftamainee.glucolog.ui.chart.drawMealMarkers
import com.koftamainee.glucolog.ui.chart.drawRangeBand
import com.koftamainee.glucolog.ui.chart.drawRoamBolusDecay
import com.koftamainee.glucolog.ui.chart.drawXGrid
import com.koftamainee.glucolog.ui.chart.drawYGrid
import java.time.LocalDate
import kotlin.math.floor

internal const val INITIAL_PX_PER_HOUR = 34f
private const val MIN_PX_PER_HOUR = 6f
private const val MAX_PX_PER_HOUR = 200f

@Composable
fun RoamChart(
    model: RoamModel,
    centerHour: Float,
    pxPerHour: Float,
    onCenterHour: (Float) -> Unit,
    onPxPerHour: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val colors = chartColors(dark)
    val measurer = rememberTextMeasurer()

    val latestModel by rememberUpdatedState(model)
    val latestCenterHour by rememberUpdatedState(centerHour)
    val latestPxPerHour by rememberUpdatedState(pxPerHour)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var px = latestPxPerHour
                        var center = latestCenterHour
                        var panAccum = Offset.Zero
                        var zoomAccum = 1f
                        var moved = false
                        val w = size.width.toFloat()
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) break
                            val panChange = event.calculatePan()
                            val zoomChange = event.calculateZoom()
                            if (!moved) {
                                panAccum += panChange
                                zoomAccum *= zoomChange
                                val panMoved = panAccum.getDistance() > viewConfiguration.touchSlop
                                val zoomMoved = kotlin.math.abs(1 - zoomAccum) > 0.01f
                                moved = panMoved || zoomMoved
                            }
                            if (moved) {
                                val focal = event.calculateCentroid(useCurrent = false).x
                                val pxNew = (px * zoomChange).coerceIn(MIN_PX_PER_HOUR, MAX_PX_PER_HOUR)
                                val viewportStart = center - w / 2f / px
                                val hourFocal = viewportStart + focal / px
                                center = (hourFocal + w / 2f / pxNew - (focal + panChange.x) / pxNew)
                                    .coerceIn(latestModel.startHour, latestModel.endHour)
                                px = pxNew
                                onPxPerHour(px)
                                onCenterHour(center)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    }
                },
        ) {
            drawRoam(latestModel, colors, measurer, latestPxPerHour, latestCenterHour)
        }
    }
}

private fun DrawScope.drawRoam(
    model: RoamModel,
    colors: com.koftamainee.glucolog.ui.chart.ChartColors,
    measurer: TextMeasurer,
    pxPerHour: Float,
    centerHour: Float,
) {
    val w = size.width
    val h = size.height
    val viewportStart = centerHour - w / 2f / pxPerHour
    val viewportEnd = centerHour + w / 2f / pxPerHour
    val toX = { hour: Float -> (hour - viewportStart) * pxPerHour }
    val toY = { g: Float -> h - (g - MIN_G) / (MAX_G - MIN_G) * h }

    drawYGrid(colors, measurer, toY, h)

    var hour = floor(viewportStart / 6f) * 6f
    while (hour <= viewportEnd) {
        drawXGrid(colors, toX, h, listOf(hour))
        hour += 6f
    }

    model.days.forEach { date ->
        val dayHour = date.toEpochDay() * 24f
        if (dayHour in viewportStart..viewportEnd) {
            drawLine(colors.grid.copy(alpha = 0.7f), Offset(toX(dayHour), 0f), Offset(toX(dayHour), h), 0.75.dp.toPx())
            val text = measurer.measure(
                AnnotatedString(dayLabel(date)),
                TextStyle(fontSize = 9.sp, color = colors.text),
            )
            drawText(text, topLeft = Offset(toX(dayHour) + 2.dp.toPx(), 2.dp.toPx()))
        }
    }

    drawRangeBand(colors, toY, w)

    val valueStyle = TextStyle(fontSize = 9.sp, color = colors.text)
    drawRoamBolusDecay(model.bolus, colors.bolus, toX, toY)
    drawBolusMarkers(model.bolus, colors.bolus, toX, toY, measurer, valueStyle)
    drawBasalMarkers(model.basal, colors.basal, toX, toY, measurer, valueStyle)
    drawGlucoseLine(model.line, colors.glucose, toX, toY)
    drawManualPoints(model.manual, colors.manual, measurer, valueStyle, toX, toY)

    drawMealMarkers(
        model.meals,
        colors.meal,
        toX,
        toY,
        measurer,
        TextStyle(fontSize = 8.sp, color = colors.text),
    )
}

private fun dayLabel(date: LocalDate): String =
    "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthValue.toString().padStart(2, '0')}"
