package com.koftamainee.glucolog.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.ui.components.DebouncedOutlinedTextField
import com.koftamainee.glucolog.ui.theme.ChartBasal
import com.koftamainee.glucolog.ui.theme.ChartBasalDark
import com.koftamainee.glucolog.ui.theme.ChartBolus
import com.koftamainee.glucolog.ui.theme.ChartBolusDark
import com.koftamainee.glucolog.ui.theme.ChartManual
import com.koftamainee.glucolog.ui.theme.ChartManualDark
import com.koftamainee.glucolog.ui.theme.ChartMeal
import com.koftamainee.glucolog.ui.theme.ChartMealDark

@Composable
fun ChartSettingsScreen(
    viewModel: ChartSettingsViewModel,
    onBack: () -> Unit,
) {
    val markerLines by viewModel.markerLines.collectAsState()
    val targetRange by viewModel.targetRange.collectAsState()
    val markerLevels by viewModel.markerLevels.collectAsState()

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text("Настройки графика", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = "Пунктирные линии ручных меток",
                style = MaterialTheme.typography.titleMedium,
            )

            MarkerLineSwitchRow(
                label = "Ручная глюкоза",
                checked = markerLines.manual,
                onCheckedChange = viewModel::setMarkerLineManual,
                dotColor = ChartManual,
                dotColorDark = ChartManualDark,
            )
            MarkerLineSwitchRow(
                label = "Приёмы пищи",
                checked = markerLines.meal,
                onCheckedChange = viewModel::setMarkerLineMeal,
                dotColor = ChartMeal,
                dotColorDark = ChartMealDark,
            )
            MarkerLineSwitchRow(
                label = "Болюс — начало",
                checked = markerLines.bolusStart,
                onCheckedChange = viewModel::setMarkerLineBolusStart,
                dotColor = ChartBolus,
                dotColorDark = ChartBolusDark,
            )
            MarkerLineSwitchRow(
                label = "Болюс — конец",
                checked = markerLines.bolusEnd,
                onCheckedChange = viewModel::setMarkerLineBolusEnd,
                dotColor = ChartBolus,
                dotColorDark = ChartBolusDark,
            )
            MarkerLineSwitchRow(
                label = "Базальный инсулин",
                checked = markerLines.basal,
                onCheckedChange = viewModel::setMarkerLineBasal,
                dotColor = ChartBasal,
                dotColorDark = ChartBasalDark,
            )

            Text(
                text = "Целевой диапазон",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DebouncedOutlinedTextField(
                    value = fmt(targetRange.lo),
                    onCommit = { value ->
                        value.toFloatOrNull()?.let { v -> viewModel.setTargetLo(v) }
                    },
                    placeholder = "Нижняя",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                DebouncedOutlinedTextField(
                    value = fmt(targetRange.hi),
                    onCommit = { value ->
                        value.toFloatOrNull()?.let { v -> viewModel.setTargetHi(v) }
                    },
                    placeholder = "Верхняя",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "Уровень отображения на графике",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DebouncedOutlinedTextField(
                    value = fmt(markerLevels.basal),
                    onCommit = { value ->
                        value.toFloatOrNull()?.let { v -> viewModel.setMarkerLevelBasal(v) }
                    },
                    label = "Базальный",
                    placeholder = "Базальный",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                DebouncedOutlinedTextField(
                    value = fmt(markerLevels.meal),
                    onCommit = { value ->
                        value.toFloatOrNull()?.let { v -> viewModel.setMarkerLevelMeal(v) }
                    },
                    label = "Приёмы пищи",
                    placeholder = "Приёмы пищи",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "Уровень в мм/л, на котором точки базального инсулина и приёмов пищи рисуются на графике.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun fmt(v: Float): String = String.format(java.util.Locale.ROOT, "%.1f", v)

@Composable
private fun MarkerLineSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    dotColor: Color,
    dotColorDark: Color,
) {
    val dark = isSystemInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 10.dp)
                .size(10.dp)
                .background(if (dark) dotColorDark else dotColor, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
