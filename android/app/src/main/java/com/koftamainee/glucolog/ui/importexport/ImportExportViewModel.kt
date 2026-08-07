package com.koftamainee.glucolog.ui.importexport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koftamainee.glucolog.data.DayRepository
import com.koftamainee.glucolog.data.importexport.CsvCodec
import com.koftamainee.glucolog.data.importexport.FileOps
import com.koftamainee.glucolog.data.importexport.ImportedFile
import com.koftamainee.glucolog.data.importexport.ImportCoordinator
import com.koftamainee.glucolog.data.importexport.JsonCodec
import com.koftamainee.glucolog.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ExportKind { JSON, CSV }

class ImportExportViewModel(
    private val repo: DayRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _pending = MutableStateFlow<ImportedFile?>(null)
    val pending: StateFlow<ImportedFile?> = _pending

    private val _needStrategy = MutableStateFlow(false)
    val needStrategy: StateFlow<Boolean> = _needStrategy

    fun export(kind: ExportKind, uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val days = repo.allDays()
                val text = when (kind) {
                    ExportKind.JSON -> JsonCodec.export(days)
                    ExportKind.CSV -> CsvCodec.export(days)
                }
                FileOps.writeText(appContext, uri, text)
                _message.value = "Экспортировано ${days.size} ${plural(days.size)}"
            } catch (e: Exception) {
                _message.value = e.message ?: "Ошибка экспорта"
            } finally {
                _busy.value = false
            }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val text = FileOps.readText(appContext, uri)
                val file = ImportCoordinator.parse(text)
                _pending.value = file
                _needStrategy.value = repo.hasData()
                if (!_needStrategy.value) applyImport(replace = true)
            } catch (e: Exception) {
                _message.value = e.message ?: "Не удалось импортировать"
                _pending.value = null
                _needStrategy.value = false
            } finally {
                _busy.value = false
            }
        }
    }

    fun applyImport(replace: Boolean) {
        val file = _pending.value ?: return
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.importDays(file.days, replace)
                val fmt = if (file.isNewFormat) "новый (с источником)" else "веб"
                _message.value = "Импортировано ${file.days.size} ${plural(file.days.size)} " +
                    "(формат: $fmt)"
            } catch (e: Exception) {
                _message.value = e.message ?: "Не удалось импортировать"
            } finally {
                _pending.value = null
                _needStrategy.value = false
                _busy.value = false
            }
        }
    }

    fun cancelImport() {
        _pending.value = null
        _needStrategy.value = false
    }

    private fun plural(n: Int): String = when {
        n % 10 == 1 && n % 100 != 11 -> "день"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "дня"
        else -> "дней"
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                ImportExportViewModel(container.dayRepository, container.appContext)
            }
        }
    }
}
