package com.koftamainee.glucolog.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun DebouncedOutlinedTextField(
    value: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
) {
    var text by rememberSaveable { mutableStateOf(value) }
    var dirty by remember(value) { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (value != text && !dirty) text = value
    }

    LaunchedEffect(text) {
        if (!dirty) return@LaunchedEffect
        dirty = false
        delay(500)
        onCommit(text)
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            dirty = true
        },
        placeholder = { Text(placeholder) },
        minLines = minLines,
        modifier = modifier,
    )
}
