package com.koftamainee.glucolog.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BackupScreen(viewModel: BackupViewModel, onBack: () -> Unit) {
    val busy by viewModel.busy.collectAsState()
    val message by viewModel.message.collectAsState()
    val googleEmail by viewModel.googleEmail.collectAsState()
    val backupEnabled by viewModel.backupEnabled.collectAsState()
    val backupDays by viewModel.backupDays.collectAsState()
    val backupProducts by viewModel.backupProducts.collectAsState()
    val backupLastTime by viewModel.backupLastTime.collectAsState()
    val backupLastError by viewModel.backupLastError.collectAsState()
    val intervalText by viewModel.intervalText.collectAsState()
    val keepCountText by viewModel.keepCountText.collectAsState()
    val authResolution by viewModel.authResolution.collectAsState()
    val checking by viewModel.checking.collectAsState()

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onAuthorizationResolutionDone(result.resultCode, result.data)
    }

    LaunchedEffect(authResolution) {
        val sender = authResolution ?: return@LaunchedEffect
        authLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }

    val contentAlpha = if (backupEnabled) 1f else 0.4f

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
                Text("Резервное копирование", style = MaterialTheme.typography.titleLarge)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Switch(
                    checked = backupEnabled,
                    onCheckedChange = viewModel::setBackupEnabled,
                )
                Spacer(Modifier.width(8.dp))
                Text("Автобэкап на Google Диск")
            }

            if (googleEmail != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Аккаунт: $googleEmail",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = viewModel::signOut,
                        enabled = !busy,
                    ) { Text("Выйти") }
                }
            } else {
                Text(
                    "Google Диск не привязан — включите автобэкап, " +
                        "авторизация пройдёт автоматически",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (checking) {
                Text(
                    "Проверка доступа к Google Диску…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = false,
                        onClick = {},
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { if (backupEnabled) viewModel.setIntervalText(it) },
                    label = { Text("Интервал (часы)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = backupEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = keepCountText,
                    onValueChange = { if (backupEnabled) viewModel.setKeepCountText(it) },
                    label = { Text("Хранить бэкапов на Диске") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = backupEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = backupDays,
                        onCheckedChange = { if (backupEnabled) viewModel.setBackupDays(it) },
                        enabled = backupEnabled,
                    )
                    Text("Дневник")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = backupProducts,
                        onCheckedChange = { if (backupEnabled) viewModel.setBackupProducts(it) },
                        enabled = backupEnabled,
                    )
                    Text("Продукты")
                }

                Button(
                    onClick = viewModel::backupNow,
                    enabled = backupEnabled && !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Бэкап сейчас") }

                if (backupLastTime > 0L) {
                    Text(
                        "Последний бэкап: ${formatTime(backupLastTime)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                backupLastError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (busy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            message?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatTime(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
