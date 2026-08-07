package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.runtime.Composable
import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.ui.components.SectionCard
import com.koftamainee.glucolog.ui.components.ToggleChipRow

@Composable
fun StoolSection(
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    SectionCard(title = "Стул") {
        ToggleChipRow(
            options = Constants.STOOL_OPTS,
            selected = selected,
            onToggle = onToggle,
        )
    }
}
