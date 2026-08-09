package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.data.MarkerLevelSettings
import com.koftamainee.glucolog.data.MarkerLineSettings
import com.koftamainee.glucolog.data.TargetRangeSettings
import com.koftamainee.glucolog.domain.ChartModel
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.domain.DayStats
import com.koftamainee.glucolog.domain.currentTimeString
import com.koftamainee.glucolog.domain.fmt1
import com.koftamainee.glucolog.domain.timeToFloat
import com.koftamainee.glucolog.ui.chart.GlucoseChart
import com.koftamainee.glucolog.ui.components.PrimaryAddButton
import com.koftamainee.glucolog.ui.components.SectionCard
import com.koftamainee.glucolog.ui.components.TimeField
import com.koftamainee.glucolog.ui.theme.ChartBasal
import com.koftamainee.glucolog.ui.theme.ChartBasalDark
import com.koftamainee.glucolog.ui.theme.ChartBolus
import com.koftamainee.glucolog.ui.theme.ChartBolusDark
import com.koftamainee.glucolog.ui.theme.ChartGlucoseDark
import com.koftamainee.glucolog.ui.theme.ChartManual
import com.koftamainee.glucolog.ui.theme.ChartManualDark
import com.koftamainee.glucolog.ui.theme.GlucologGreen
import java.time.LocalDate

@Composable
fun GlucoseSection(
    data: DayData,
    stats: DayStats?,
    markerLines: MarkerLineSettings,
    markerLevels: MarkerLevelSettings,
    targetRange: TargetRangeSettings,
    onAdd: (h: Float, g: Float) -> Unit,
    onOpenRoam: (LocalDate) -> Unit,
) {
    val chart = remember(data) { ChartModel.from(data) }

    SectionCard(title = "Глюкоза и инсулин", trailing = {
        IconButton(onClick = { onOpenRoam(data.date) }) {
            Icon(Icons.Filled.Fullscreen, contentDescription = "Развернуть график")
        }
    }) {
        GlucoseChart(chart = chart, markerLines = markerLines, markerLevels = markerLevels, targetRange = targetRange)
        ChartXLabels()
        ChartLegend()
        StatsRow(stats = stats)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            var value by rememberSaveable { mutableStateOf("") }
            var time by rememberSaveable { mutableStateOf(currentTimeString()) }
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("мм/л") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            TimeField(
                value = time,
                onValue = { time = it },
                label = "Время",
                modifier = Modifier.width(96.dp),
            )
            PrimaryAddButton(
                text = "Добавить",
                onClick = {
                    val g = value.toFloatOrNull()
                    val h = timeToFloat(time)
                    if (g != null && g in 1f..30f && h != null) {
                        onAdd(h, g)
                        value = ""
                        time = currentTimeString()
                    }
                },
            )
        }
    }
}

@Composable
private fun ChartLegend() {
    val dark = isSystemInDarkTheme()
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LegendItem(if (dark) ChartGlucoseDark else GlucologGreen, "Глюкоза")
        LegendItem(if (dark) ChartManualDark else ChartManual, "Ручная")
        LegendItem(if (dark) ChartBolusDark else ChartBolus, "Болюс")
        LegendItem(if (dark) ChartBasalDark else ChartBasal, "Базальный")
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = color,
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChartXLabels() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("0:00", "6:00", "12:00", "18:00", "24:00").forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsRow(stats: DayStats?) {
    if (stats == null) return
    val dark = isSystemInDarkTheme()
    val hypoColor = if (dark) ChartBasalDark else ChartBasal
    val hyperColor = if (dark) ChartBolusDark else ChartBolus

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("n=${stats.n}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("\u2193${fmt1(stats.min)} \u2191${fmt1(stats.max)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("\u00D8${fmt1(stats.avg)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("SD=${fmt1(stats.sd)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (stats.hypo > 0) {
            Text("\u2193${stats.hypo}", style = MaterialTheme.typography.bodySmall, color = hypoColor)
        }
        if (stats.hyper > 0) {
            Text("\u2191${stats.hyper}", style = MaterialTheme.typography.bodySmall, color = hyperColor)
        }
    }
}
