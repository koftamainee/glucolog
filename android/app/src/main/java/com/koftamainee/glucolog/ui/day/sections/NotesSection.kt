package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.koftamainee.glucolog.ui.components.SectionCard
import kotlinx.coroutines.delay

@Composable
fun NotesCard(initial: String, onCommit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    var pending by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initial) { if (initial != text && !pending) text = initial }

    LaunchedEffect(text) {
        if (!pending) return@LaunchedEffect
        pending = false
        delay(500)
        onCommit(text)
    }

    SectionCard(title = "Заметки") {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                pending = true
            },
            placeholder = { Text("Заметки за день") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun ConclusionsCard(initial: String, onCommit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    var pending by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initial) { if (initial != text && !pending) text = initial }

    LaunchedEffect(text) {
        if (!pending) return@LaunchedEffect
        pending = false
        delay(500)
        onCommit(text)
    }

    SectionCard(title = "Выводы дня") {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                pending = true
            },
            placeholder = { Text("Выводы") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
