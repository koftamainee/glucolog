package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.runtime.Composable
import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.ui.components.ChipRow
import com.koftamainee.glucolog.ui.components.SectionCard

@Composable
fun StressSection(
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    SectionCard(title = "Стресс") {
        ChipRow(
            options = Constants.STRESS_OPTS,
            selected = selected,
            onSelect = { opt -> onSelect(if (selected == opt) null else opt) },
        )
    }
}
