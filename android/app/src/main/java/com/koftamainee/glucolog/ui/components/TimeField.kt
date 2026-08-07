package com.koftamainee.glucolog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { showPicker = true },
        )
    }

    if (showPicker) {
        val initial = parseTime(value)
        val state = rememberTimePickerState(
            initialHour = initial.first,
            initialMinute = initial.second,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismissRequest = { showPicker = false },
            title = {},
            confirmButton = {
                TextButton(onClick = {
                    onValue(formatTime(state.hour, state.minute))
                    showPicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Отмена") }
            },
        ) {
            TimePicker(state = state)
        }
    }
}

private fun parseTime(time: String): Pair<Int, Int> {
    val parts = time.split(":")
    val now = LocalTime.now()
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: now.hour
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}

private fun formatTime(hour: Int, minute: Int): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
