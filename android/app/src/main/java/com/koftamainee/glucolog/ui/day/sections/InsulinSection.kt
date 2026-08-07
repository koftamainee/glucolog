package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.domain.currentTimeString
import com.koftamainee.glucolog.domain.timeToFloat
import com.koftamainee.glucolog.ui.components.PrimaryAddButton
import com.koftamainee.glucolog.ui.components.SectionCard
import com.koftamainee.glucolog.ui.components.TimeField

@Composable
fun BolusCard(onAdd: (h: Float, value: Float) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf(currentTimeString()) }

    SectionCard(title = "Болюс") {
        InsulinForm(
            value = value,
            time = time,
            onValue = { value = it },
            onTime = { time = it },
            onAdd = {
                val v = value.toFloatOrNull()
                val h = timeToFloat(time)
                if (v != null && v > 0f && h != null) {
                    onAdd(h, v)
                    value = ""
                    time = currentTimeString()
                }
            },
        )
    }
}

@Composable
fun BasalCard(onAdd: (h: Float, value: Float) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf(currentTimeString()) }

    SectionCard(title = "Базальный") {
        InsulinForm(
            value = value,
            time = time,
            onValue = { value = it },
            onTime = { time = it },
            onAdd = {
                val v = value.toFloatOrNull()
                val h = timeToFloat(time)
                if (v != null && v > 0f && h != null) {
                    onAdd(h, v)
                    value = ""
                    time = currentTimeString()
                }
            },
        )
    }
}

@Composable
private fun InsulinForm(
    value: String,
    time: String,
    onValue: (String) -> Unit,
    onTime: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            label = { Text("ед.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        TimeField(
            value = time,
            onValue = onTime,
            label = "Время",
            modifier = Modifier.width(96.dp),
        )
        PrimaryAddButton(
            text = "Добавить",
            onClick = onAdd,
        )
    }
}
