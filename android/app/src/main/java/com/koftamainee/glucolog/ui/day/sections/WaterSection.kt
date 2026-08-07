package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.ui.components.SectionCard

@Composable
fun WaterSection(
    data: DayData,
    onSet: (Int) -> Unit,
) {
    val filled = data.day?.water ?: 0

    SectionCard(title = "Вода") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 1..Constants.WATER_GLASSES) {
                val isFilled = i <= filled
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            val next = if (i <= filled) i - 1 else i
                            onSet(next)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = if (isFilled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isFilled) Color.Transparent else MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "▾",
                                color = if (isFilled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
