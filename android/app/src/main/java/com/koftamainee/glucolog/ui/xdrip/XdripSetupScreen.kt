package com.koftamainee.glucolog.ui.xdrip

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.koftamainee.glucolog.domain.formatDateLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun XdripSetupScreen(viewModel: XdripSetupViewModel, onBack: () -> Unit) {
    val status by viewModel.status.collectAsState()
    val checking by viewModel.checking.collectAsState()
    val message by viewModel.checkMessage.collectAsState()
    val xiaomiEnabled by viewModel.xiaomiEnabled.collectAsState()
    val watchStats by viewModel.watchStats.collectAsState()
    val forceMessage by viewModel.forceMessage.collectAsState()

    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

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
                Text("Настройка xDrip", style = MaterialTheme.typography.titleLarge)
            }

            Text("Статус", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (status.connected) "Подключено к xDrip" else "Не настроено",
                style = MaterialTheme.typography.bodyMedium,
                color = if (status.connected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            status.lastValue?.let { value ->
                val dateLabel = status.lastDate?.let {
                    runCatching { formatDateLabel(LocalDate.parse(it)) }.getOrNull()
                }
                Text(
                    text = "Последнее чтение: $dateLabel ${status.lastTime.orEmpty()} — " +
                        "%.1f ммоль/л".format(value),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = viewModel::sync,
                enabled = !checking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (checking) "Синхронизация…" else "Синхронизировать")
            }

            message?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            Text("Часы Xiaomi (WatchDrip)", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Отправлять показания на часы",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (xiaomiEnabled) "Сервис запущен" else "Выключено",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = xiaomiEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.setXiaomiEnabled(enabled)
                    },
                )
            }
            Text(
                text = "1. Спарите часы с Mi Fitness.\n" +
                    "2. Установите на часы приложение WatchDrip из магазина часов.\n" +
                    "3. Включите тумблер.\n" +
                    "4. Разрешите уведомления и доступ к Mi Fitness.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Отправлено: ${watchTime(watchStats.lastSentMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Подтверждено с часов: ${watchTime(watchStats.lastConfirmedMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = viewModel::forceX,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Синхронизировать с xDrip сейчас")
            }
            forceMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            Text("Инструкция", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "1. Установите xDrip и подключите сенсор.\n" +
                    "2. В xDrip: ⋮ → «Настройки» → «Расширенные настройки» → включите " +
                    "«API службы трансляции» (Broadcast Service API).\n" +
                    "3. Для истории: включите «Веб служба xDrip» (порт 127.0.0.1:17580); " +
                    "«Открытая веб служба» — выключена, секрет не нужен.\n" +
                    "4. Нажмите «Синхронизировать». Подтянется до 1000 последних показаний " +
                    "(~3.5 дня — это максимум xDrip), дальше — только новые показания.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun watchTime(ms: Long): String {
    if (ms <= 0L) return "—"
    return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm dd.MM"))
}
