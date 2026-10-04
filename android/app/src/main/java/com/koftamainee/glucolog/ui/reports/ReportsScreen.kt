package com.koftamainee.glucolog.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.domain.RangeBand
import com.koftamainee.glucolog.domain.RangeBandKind
import com.koftamainee.glucolog.domain.ReportModel
import com.koftamainee.glucolog.domain.fmtDurationMinutes
import com.koftamainee.glucolog.domain.fmtG
import com.koftamainee.glucolog.domain.fmtG2
import com.koftamainee.glucolog.domain.fmtPct
import com.koftamainee.glucolog.domain.pluralDays
import com.koftamainee.glucolog.domain.tirBarSegments
import com.koftamainee.glucolog.ui.components.SectionCard
import com.koftamainee.glucolog.ui.components.SelectChip
import com.koftamainee.glucolog.ui.theme.GlucologGreen
import com.koftamainee.glucolog.ui.theme.WarnBg
import com.koftamainee.glucolog.ui.theme.WarnText

private val LowColor = Color(0xFFE05A33)
private val VeryLowColor = Color(0xFFC62828)
private val HighColor = Color(0xFFE8A33D)
private val VeryHighColor = Color(0xFF7B1FA2)

private data class Help(val title: String, val text: String)

private val HBA1C_HELP = Help(
    "HbA1c",
    "HbA1c — гликированный гемоглобин, отражает среднюю глюкозу за примерно 2–3 месяца. " +
        "В отчёте он расчётный: вычислен по средней глюкозе за период по формуле ADAG и может " +
        "отличаться от лабораторного анализа.",
)

private val GMI_HELP = Help(
    "GMI",
    "GMI — индикатор управления глюкозой (Glucose Management Indicator). Это расчётная оценка " +
        "HbA1c по данным непрерывного мониторинга глюкозы за период наблюдения.",
)

private val SD_CV_HELP = Help(
    "SD / CV",
    "SD — стандартное отклонение, CV — коэффициент вариации (SD ÷ средняя × 100%). " +
        "Чем выше эти показатели, тем сильнее колеблется глюкоза в течение периода.",
)

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) viewModel.export(uri)
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
            Text("Отчёты", style = MaterialTheme.typography.titleLarge)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.periods.forEach { period ->
                    SelectChip(
                        label = period.label,
                        selected = state.days == period.days,
                        onSelect = { viewModel.setDays(period.days) },
                    )
                }
            }

            Button(
                onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                enabled = state.report != null && !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Сохранить PDF") }

            if (state.busy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            val report = state.report
            if (report == null) {
                if (!state.busy && state.error == null) {
                    Text("Нет данных за выбранный период.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                SummaryPreview(report)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SummaryPreview(report: ReportModel) {
    var help by remember { mutableStateOf<Help?>(null) }

    if (!report.clinicallyValid) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WarnBg,
            contentColor = WarnText,
        ) {
            Text(
                text = "Данных недостаточно для клинической интерпретации: " +
                    "${report.daysWithData} ${pluralDays(report.daysWithData)} с данными " +
                    "(${fmtPct(report.coveragePercent)} от периода; нужно ≥${report.requiredDays} " +
                    "${pluralDays(report.requiredDays)}).",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
    }

    SectionCard("Гликемический контроль") {
        MetricRow("Средняя глюкоза", "${fmtG(report.meanG)} ммоль/л")
        MetricRow(
            "Расчётный HbA1c",
            "${fmtG(report.hba1cPercent)}% · ${fmtG(report.hba1cMmolMol)} ммоль/моль",
            info = HBA1C_HELP,
            onInfo = { help = it },
        )
        MetricRow("GMI", "${fmtG(report.gmiPercent)}%", info = GMI_HELP, onInfo = { help = it })
        MetricRow(
            "SD / CV",
            "${fmtG(report.sdG)} / ${fmtG(report.cvPercent)}%",
            info = SD_CV_HELP,
            onInfo = { help = it },
        )
        MetricRow(
            "Показаний за период",
            "${report.readings} · CGM ${report.cgmReadings}, глюкометр ${report.manualReadings}",
        )
    }

    SectionCard("Время в диапазоне") {
        TirBar(report.bands)
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        report.bands.forEach { band ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(band.kind.title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${fmtPct(band.percent)} · ${fmtDurationMinutes(band.perDayMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    val slots = buildList {
        report.fasting?.let { add("Натощак (03:00–08:00)" to it) }
        report.postprandial?.takeIf { it.readings > 0 }?.let {
            add("После еды (1–3 ч)" to it)
        }
    }
    if (slots.isNotEmpty()) {
        SectionCard("Натощак и после еды") {
            slots.forEach { (label, slot) ->
                MetricRow(label, "средняя ${fmtG(slot.meanG)}, пик ${fmtG(slot.maxG)} · ${slot.readings} изм.")
            }
        }
    }

    SectionCard("Инсулинотерапия и питание") {
        MetricRow("Суточная доза инсулина", "${fmtG(report.tddPerDay)} ед. (базал ${fmtG(report.basalPerDay)}, болюс ${fmtG(report.bolusPerDay)})")
        MetricRow("Углеводы в сутки", "${fmtG(report.carbsPerDay)} г · ${fmtG2(report.bolusCarbRatio)} ед./г")
        MetricRow("Гипогликемий (всего)", "${report.hypoCount}")
        MetricRow("Гипогликемий (ночных)", "${report.nightHypoCount}")
        MetricRow("Гипергликемий (всего)", "${report.hyperCount}")
        MetricRow("Гипергликемий (ночных)", "${report.nightHyperCount}")
    }

    help?.let { current ->
        AlertDialog(
            onDismissRequest = { help = null },
            title = { Text(current.title) },
            text = { Text(current.text) },
            confirmButton = {
                TextButton(onClick = { help = null }) { Text("Понятно") }
            },
        )
    }
}

@Composable
private fun TirBar(bands: List<RangeBand>) {
    Row(modifier = Modifier.fillMaxWidth().height(14.dp)) {
        tirBarSegments(bands).forEach { band ->
            Box(
                modifier = Modifier
                    .weight(band.percent)
                    .height(14.dp)
                    .background(bandColor(band.kind)),
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    info: Help? = null,
    onInfo: (Help) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        androidx.compose.foundation.layout.Spacer(Modifier.weight(0.3f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.4f),
        )
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (info != null) {
                IconButton(onClick = { onInfo(info) }, modifier = Modifier.size(20.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Справка: ${info.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun bandColor(kind: RangeBandKind): Color = when (kind) {
    RangeBandKind.VERY_LOW -> VeryLowColor
    RangeBandKind.LOW -> LowColor
    RangeBandKind.TARGET -> GlucologGreen
    RangeBandKind.HIGH -> HighColor
    RangeBandKind.VERY_HIGH -> VeryHighColor
}