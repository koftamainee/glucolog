package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.domain.currentTimeString
import com.koftamainee.glucolog.domain.timeToFloat
import com.koftamainee.glucolog.ui.components.PrimaryAddButton
import com.koftamainee.glucolog.ui.components.SectionCard

@Composable
fun InsulinSection(
    data: DayData,
    onAddBolus: (h: Float, value: Float) -> Unit,
    onAddBasal: (h: Float, value: Float) -> Unit,
) {
    var bolus by rememberSaveable { mutableStateOf("") }
    var bolusTime by rememberSaveable { mutableStateOf(currentTimeString()) }
    var basal by rememberSaveable { mutableStateOf("") }
    var basalTime by rememberSaveable { mutableStateOf(currentTimeString()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Болюс") {
            InsulinForm(
                value = bolus,
                time = bolusTime,
                onValue = { bolus = it },
                onTime = { bolusTime = it },
                onAdd = {
                    val v = bolus.toFloatOrNull()
                    val h = timeToFloat(bolusTime)
                    if (v != null && v > 0f && h != null) {
                        onAddBolus(h, v)
                        bolus = ""
                        bolusTime = currentTimeString()
                    }
                },
            )
        }
        SectionCard(title = "Базальный") {
            InsulinForm(
                value = basal,
                time = basalTime,
                onValue = { basal = it },
                onTime = { basalTime = it },
                onAdd = {
                    val v = basal.toFloatOrNull()
                    val h = timeToFloat(basalTime)
                    if (v != null && v > 0f && h != null) {
                        onAddBasal(h, v)
                        basal = ""
                        basalTime = currentTimeString()
                    }
                },
            )
        }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            label = { Text("ед.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = time,
            onValueChange = onTime,
            label = { Text("Время") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(120.dp),
        )
        PrimaryAddButton(
            text = "Добавить",
            modifier = Modifier.width(130.dp),
            onClick = onAdd,
        )
    }
}
