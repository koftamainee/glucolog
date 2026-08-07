package com.koftamainee.glucolog.ui.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun ImportExportScreen(viewModel: ImportExportViewModel) {
    val busy by viewModel.busy.collectAsState()
    val message by viewModel.message.collectAsState()
    val pending by viewModel.pending.collectAsState()
    val needStrategy by viewModel.needStrategy.collectAsState()

    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.export(ExportKind.JSON, uri)
    }
    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) viewModel.export(ExportKind.CSV, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.import(uri)
    }

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
            Text("Данные", style = MaterialTheme.typography.titleLarge)

            Text("Экспорт", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { jsonExportLauncher.launch("glucolog-${LocalDate.now()}.json") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Экспорт JSON") }
            Button(
                onClick = { csvExportLauncher.launch("glucolog-${LocalDate.now()}.csv") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Экспорт CSV") }

            Spacer(Modifier.height(4.dp))

            Text("Импорт", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Импорт JSON / CSV") }
            Text(
                text = "Глюкоза из файла не импортируется",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (busy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            message?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (pending != null && needStrategy) {
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("Импорт данных") },
            text = { Text("В приложении уже есть данные. Как применить импортируемые дни?") },
            confirmButton = {
                TextButton(onClick = { viewModel.applyImport(replace = true) }) { Text("Заменить") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.applyImport(replace = false) }) { Text("Объединить") }
            },
        )
    }
}
