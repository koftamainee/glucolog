package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.domain.currentTimeString
import com.koftamainee.glucolog.domain.timeToFloat
import com.koftamainee.glucolog.ui.components.PrimaryAddButton
import com.koftamainee.glucolog.ui.components.SectionCard

@Composable
fun GlucoseSection(
    data: DayData,
    onAdd: (h: Float, g: Float) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf(currentTimeString()) }

    SectionCard(title = "Глюкоза") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("ммоль/л") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Время") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp),
            )
            PrimaryAddButton(
                text = "Добавить",
                modifier = Modifier.width(130.dp),
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

        Text(
            text = "График появится в следующем обновлении.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
