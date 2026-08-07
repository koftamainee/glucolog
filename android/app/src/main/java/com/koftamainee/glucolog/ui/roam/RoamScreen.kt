package com.koftamainee.glucolog.ui.roam

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.domain.RoamModel
import com.koftamainee.glucolog.domain.formatDateLabel
import java.time.LocalDate

@Composable
fun RoamScreen(
    viewModel: RoamViewModel,
    onBack: () -> Unit,
) {
    val model by viewModel.model.collectAsState()
    var pxPerHour by remember { mutableStateOf(INITIAL_PX_PER_HOUR) }
    var centerHour by remember { mutableStateOf(viewModel.openedCenterHour) }

    LaunchedEffect(model.startHour, model.endHour) {
        if (model !== RoamModel.EMPTY) {
            centerHour = centerHour.coerceIn(model.startHour, model.endHour)
        }
    }
    LaunchedEffect(centerHour) {
        viewModel.onViewportChanged(centerHour)
    }

    val currentDay = remember(centerHour) {
        LocalDate.ofEpochDay((centerHour / 24f).toLong())
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                    Text(
                        text = "График",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        centerHour = LocalDate.now().toEpochDay() * 24f + 12f
                    }) {
                        Icon(Icons.Filled.Today, contentDescription = "Сегодня")
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
                    }
                }
            }
        },
        bottomBar = {
            Surface {
                Text(
                    text = formatDateLabel(currentDay),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            RoamChart(
                model = model,
                centerHour = centerHour,
                pxPerHour = pxPerHour,
                onCenterHour = { centerHour = it },
                onPxPerHour = { pxPerHour = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
