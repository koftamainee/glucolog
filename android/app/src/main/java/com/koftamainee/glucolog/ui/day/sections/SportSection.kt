package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.ui.components.SectionCard
import com.koftamainee.glucolog.ui.components.SelectChip

@Composable
fun SportSection(
    data: DayData,
    onSport: (Boolean) -> Unit,
    onSteps: (String) -> Unit,
) {
    val sport = data.day?.sport == true

    SectionCard(title = "Спорт") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SelectChip("Да", sport, { onSport(true) })
            SelectChip("Нет", !sport, { onSport(false) })
        }
        OutlinedTextField(
            value = data.day?.steps?.toString() ?: "",
            onValueChange = onSteps,
            label = { Text("Шаги") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }
}
