package com.koftamainee.glucolog.ui.reports

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koftamainee.glucolog.data.ReportRepository
import com.koftamainee.glucolog.data.importexport.FileOps
import com.koftamainee.glucolog.data.report.PdfNotifier
import com.koftamainee.glucolog.data.report.PdfReport
import com.koftamainee.glucolog.di.AppContainer
import com.koftamainee.glucolog.domain.ReportModel
import com.koftamainee.glucolog.domain.pluralDays
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReportPeriod(val days: Int) {
    val label: String = "$days ${pluralDays(days)}"
}

data class ReportsUiState(
    val days: Int = 14,
    val report: ReportModel? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

class ReportsViewModel(
    private val repo: ReportRepository,
    private val appContext: Context,
) : ViewModel() {

    private val state = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = state.asStateFlow()

    val periods = listOf(7, 14, 30, 90).map { ReportPeriod(it) }

    init {
        load()
    }

    fun setDays(value: Int) {
        if (value == state.value.days) return
        state.update { it.copy(days = value) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            state.update { it.copy(busy = true, error = null) }
            try {
                val to = LocalDate.now()
                val from = to.minusDays((state.value.days - 1).toLong())
                state.update { it.copy(report = repo.build(from, to)) }
            } catch (e: Exception) {
                state.update { it.copy(error = e.message ?: "Не удалось собрать отчёт", report = null) }
            } finally {
                state.update { it.copy(busy = false) }
            }
        }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            val model = state.value.report ?: return@launch
            state.update { it.copy(busy = true) }
            try {
                val fileName = suggestedFileName()
                val bytes = PdfReport.summaryBytes(model)
                FileOps.write(appContext, uri) { out -> out.write(bytes) }
                runCatching { PdfNotifier.notifyPdfSaved(appContext, bytes, fileName) }
                Toast.makeText(appContext, "Сохранено: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(appContext, e.message ?: "Ошибка сохранения PDF", Toast.LENGTH_SHORT).show()
            } finally {
                state.update { it.copy(busy = false) }
            }
        }
    }

    fun suggestedFileName(): String {
        val model = state.value.report
        return if (model != null) {
            PdfReport.suggestedFileName(model, FILE_PREFIX)
        } else {
            "$FILE_PREFIX.pdf"
        }
    }

    companion object {
        const val FILE_PREFIX = "glucolog-report"

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                ReportsViewModel(
                    container.reportRepository,
                    container.appContext,
                )
            }
        }
    }
}