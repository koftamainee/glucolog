package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.domain.GlucoseSource
import com.koftamainee.glucolog.domain.InsulinType
import com.koftamainee.glucolog.domain.floatToTime
import com.koftamainee.glucolog.ui.components.ConfirmDialog
import com.koftamainee.glucolog.ui.components.SectionCard

private data class LogEntry(
    val h: Float,
    val type: String,
    val value: Float,
    val glucoseId: Long? = null,
)

@Composable
fun JournalSection(
    data: DayData,
    onDeleteGlucose: (Long) -> Unit,
    onDeleteInsulin: (Float, InsulinType) -> Unit,
) {
    val entries = remember(data) {
        buildList {
            data.glucose.filter { it.source == GlucoseSource.MANUAL.dbValue }.forEach { p ->
                add(LogEntry(p.h, "glucose", p.g, glucoseId = p.id))
            }
            data.insulin.forEach { p ->
                p.bolus?.takeIf { it > 0f }?.let { add(LogEntry(p.h, "bolus", it)) }
                p.basal?.takeIf { it > 0f }?.let { add(LogEntry(p.h, "basal", it)) }
            }
        }.sortedBy { it.h }
    }

    var pendingDelete by remember { mutableStateOf<LogEntry?>(null) }

    SectionCard(title = "Журнал дня") {
        if (entries.isEmpty()) {
            Text(
                text = "Нет записей",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entries.forEach { e ->
                    JournalRow(e = e, onDelete = { pendingDelete = e })
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        val label = when (entry.type) {
            "glucose" -> "🩸 Глюкоза: %.1f ммоль/л".format(entry.value)
            "bolus" -> "💉 Болюс: %.1f ед.".format(entry.value)
            else -> "💊 Базальный: %.1f ед.".format(entry.value)
        }
        ConfirmDialog(
            title = "Удалить запись?",
            message = "$label · ${floatToTime(entry.h)}",
            onConfirm = {
                when (entry.type) {
                    "glucose" -> entry.glucoseId?.let(onDeleteGlucose)
                    "bolus" -> onDeleteInsulin(entry.h, InsulinType.BOLUS)
                    else -> onDeleteInsulin(entry.h, InsulinType.BASAL)
                }
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun JournalRow(
    e: LogEntry,
    onDelete: () -> Unit,
) {
    val (label, valueText, color) = when (e.type) {
        "glucose" -> Triple(
            "Глюкоза",
            "%.1f ммоль/л".format(e.value),
            when {
                e.value < 4f -> MaterialTheme.colorScheme.error
                e.value > 10f -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            },
        )
        "bolus" -> Triple("Болюс", "%.1f ед.".format(e.value), Color(0xFFE05A33))
        else -> Triple("Базальный", "%.1f ед.".format(e.value), Color(0xFF507FCC))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = floatToTime(e.h),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "  $label: $valueText",
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Удалить запись",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
