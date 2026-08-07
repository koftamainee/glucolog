package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.domain.calcSleepDuration
import com.koftamainee.glucolog.ui.components.SectionCard
import com.koftamainee.glucolog.ui.components.TimeField

@Composable
fun SleepSection(
    data: DayData,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit,
) {
    val start = data.day?.sleepStart ?: Constants.DEFAULT_SLEEP_START
    val end = data.day?.sleepEnd ?: Constants.DEFAULT_SLEEP_END

    SectionCard(title = "Сон") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TimeField(
                value = start,
                onValue = onStart,
                label = "Лёг",
                modifier = Modifier.weight(1f),
            )
            TimeField(
                value = end,
                onValue = onEnd,
                label = "Встал",
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = "Длительность: ${calcSleepDuration(start, end)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
