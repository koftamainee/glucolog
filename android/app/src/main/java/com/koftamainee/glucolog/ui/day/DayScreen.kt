package com.koftamainee.glucolog.ui.day

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.domain.DayTextField
import com.koftamainee.glucolog.domain.GlucoseSource
import com.koftamainee.glucolog.domain.InsulinType
import com.koftamainee.glucolog.domain.MealField
import com.koftamainee.glucolog.domain.formatDateLabel
import com.koftamainee.glucolog.ui.day.sections.GlucoseSection
import com.koftamainee.glucolog.ui.day.sections.InsulinSection
import com.koftamainee.glucolog.ui.day.sections.JournalSection
import com.koftamainee.glucolog.ui.day.sections.MealsSection
import com.koftamainee.glucolog.ui.day.sections.NotesSection
import com.koftamainee.glucolog.ui.day.sections.SleepSection
import com.koftamainee.glucolog.ui.day.sections.SportSection
import com.koftamainee.glucolog.ui.day.sections.StoolSection
import com.koftamainee.glucolog.ui.day.sections.StressSection
import com.koftamainee.glucolog.ui.day.sections.WaterSection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(viewModel: DayViewModel) {
    val date by viewModel.date.collectAsState()
    val data by viewModel.dayData.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            DayHeader(
                date = date,
                onPrev = { viewModel.shift(-1) },
                onNext = { viewModel.shift(1) },
                onDateClick = { showDatePicker = true },
            )

            val day = data
            if (day == null) {
                Text(
                    text = "Загрузка…",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlucoseSection(
                        data = day,
                        onAdd = { h, g -> viewModel.addGlucose(h, g, GlucoseSource.MANUAL) },
                    )
                    InsulinSection(
                        data = day,
                        onAddBolus = viewModel::setBolus,
                        onAddBasal = viewModel::setBasal,
                    )
                    MealsSection(
                        meals = day.meals,
                        onSetField = viewModel::setMealField,
                    )
                    WaterSection(data = day, onSet = viewModel::setWater)
                    SportSection(
                        data = day,
                        onSport = viewModel::setSport,
                        onSteps = viewModel::setSteps,
                    )
                    StoolSection(
                        selected = day.stool.map { it.option },
                        onToggle = viewModel::toggleStool,
                    )
                    SleepSection(
                        data = day,
                        onStart = viewModel::setSleepStart,
                        onEnd = viewModel::setSleepEnd,
                    )
                    StressSection(
                        selected = day.day?.stress,
                        onSelect = viewModel::setStress,
                    )
                    NotesSection(data = day, onText = viewModel::setText)
                    JournalSection(
                        data = day,
                        onDeleteGlucose = viewModel::deleteGlucose,
                        onDeleteInsulin = viewModel::removeInsulin,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.setDate(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            },
        ) {
            Column {
                TextButton(
                    onClick = {
                        viewModel.today()
                        showDatePicker = false
                    },
                    modifier = Modifier.align(Alignment.End),
                ) { Text("Сегодня") }
                DatePicker(state = pickerState)
            }
        }
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Text("‹", style = MaterialTheme.typography.headlineMedium)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDateClick,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Glucolog",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = formatDateLabel(date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNext) {
            Text("›", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
