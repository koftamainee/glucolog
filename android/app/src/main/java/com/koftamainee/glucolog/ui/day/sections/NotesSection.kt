package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.domain.DayTextField
import com.koftamainee.glucolog.ui.components.SectionCard
import kotlinx.coroutines.delay

@Composable
fun NotesSection(
    data: DayData,
    onText: (DayTextField, String) -> Unit,
) {
    var notes by rememberSaveable { mutableStateOf(data.day?.notes ?: "") }
    var conclusions by rememberSaveable { mutableStateOf(data.day?.conclusions ?: "") }

    LaunchedEffect(data.day?.notes) { notes = data.day?.notes ?: "" }
    LaunchedEffect(data.day?.conclusions) { conclusions = data.day?.conclusions ?: "" }

    var pendingNotes by rememberSaveable { mutableStateOf(false) }
    var pendingConcl by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(notes) {
        if (!pendingNotes) return@LaunchedEffect
        pendingNotes = false
        delay(500)
        onText(DayTextField.NOTES, notes)
    }
    LaunchedEffect(conclusions) {
        if (!pendingConcl) return@LaunchedEffect
        pendingConcl = false
        delay(500)
        onText(DayTextField.CONCLUSIONS, conclusions)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Заметки") {
            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                    pendingNotes = true
                },
                placeholder = { Text("Заметки за день") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SectionCard(title = "Выводы дня") {
            OutlinedTextField(
                value = conclusions,
                onValueChange = {
                    conclusions = it
                    pendingConcl = true
                },
                placeholder = { Text("Выводы") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
