package com.koftamainee.glucolog.ui.importexport

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koftamainee.glucolog.data.DayRepository
import com.koftamainee.glucolog.data.ProductRepository
import com.koftamainee.glucolog.data.SettingsDataStore
import com.koftamainee.glucolog.data.ThemeMode
import com.koftamainee.glucolog.data.backup.BackupPayload
import com.koftamainee.glucolog.data.backup.DriveAuthException
import com.koftamainee.glucolog.data.backup.DriveBackup
import com.koftamainee.glucolog.data.backup.GoogleAuthFlow
import com.koftamainee.glucolog.data.backup.GoogleDriveClient
import com.koftamainee.glucolog.data.db.ProductEntity
import com.koftamainee.glucolog.data.importexport.CsvCodec
import com.koftamainee.glucolog.data.importexport.FileOps
import com.koftamainee.glucolog.data.importexport.ImportedFile
import com.koftamainee.glucolog.data.importexport.ImportCoordinator
import com.koftamainee.glucolog.data.importexport.JsonCodec
import com.koftamainee.glucolog.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ExportKind { JSON, CSV }

class ImportExportViewModel(
    private val repo: DayRepository,
    private val productRepository: ProductRepository,
    private val appContext: Context,
    private val settings: SettingsDataStore,
    private val driveClient: GoogleDriveClient,
) : ViewModel() {

    private val authFlow = GoogleAuthFlow(appContext, settings)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _pending = MutableStateFlow<ImportedFile?>(null)
    val pending: StateFlow<ImportedFile?> = _pending

    private val _needStrategy = MutableStateFlow(false)
    val needStrategy: StateFlow<Boolean> = _needStrategy

    private val _pendingProducts = MutableStateFlow<List<ProductEntity>>(emptyList())

    private val _googleEmail = MutableStateFlow<String?>(null)
    val googleEmail: StateFlow<String?> = _googleEmail.asStateFlow()

    private val _authResolution = MutableStateFlow<IntentSender?>(null)
    val authResolution: StateFlow<IntentSender?> = _authResolution.asStateFlow()

    private val _driveBackups = MutableStateFlow<List<DriveBackup>?>(null)
    val driveBackups: StateFlow<List<DriveBackup>?> = _driveBackups.asStateFlow()

    private val _driveBackupsVisible = MutableStateFlow(false)
    val driveBackupsVisible: StateFlow<Boolean> = _driveBackupsVisible.asStateFlow()

    private var pendingAction: (suspend () -> Unit)? = null

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    init {
        viewModelScope.launch {
            _googleEmail.value = settings.backupGoogleEmail.first()
        }
    }

    fun setThemeMode(mode: ThemeMode) = launch { settings.setThemeMode(mode) }

    fun export(kind: ExportKind, uri: Uri) {
        launch {
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
        launch {
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

    fun showDriveBackups() {
        if (_googleEmail.value == null) {
            requestAuthorization {
                openDriveBackups()
            }
        } else {
            openDriveBackups()
        }
    }

    private fun openDriveBackups() {
        _driveBackupsVisible.value = true
        loadDriveBackups()
    }

    fun hideDriveBackups() {
        _driveBackupsVisible.value = false
    }

    fun loadDriveBackups() {
        launch {
            _busy.value = true
            try {
                val auth = GoogleDriveClient.authorizeSilently(appContext)
                _driveBackups.value = driveClient.listBackups(auth.accessToken)
            } catch (e: DriveAuthException) {
                settings.setBackupGoogleEmail(null)
                _googleEmail.value = null
                _message.value = "Требуется вход в Google"
                _driveBackups.value = emptyList()
            } catch (e: Exception) {
                _message.value = e.message ?: "Не удалось получить список бэкапов"
                _driveBackups.value = emptyList()
            } finally {
                _busy.value = false
            }
        }
    }

    fun importDriveBackup(backup: DriveBackup) {
        launch {
            _busy.value = true
            try {
                val auth = GoogleDriveClient.authorizeSilently(appContext)
                val text = driveClient.downloadBackup(auth.accessToken, backup.id)
                val parsed = BackupPayload.parse(text)
                if (parsed.days.isEmpty() && parsed.products.isEmpty()) {
                    _message.value = "Бэкап пуст"
                    return@launch
                }
                _pendingProducts.value = parsed.products
                if (parsed.days.isNotEmpty()) {
                    _pending.value = ImportedFile(parsed.days, isNewFormat = true)
                    _needStrategy.value = repo.hasData()
                    if (!_needStrategy.value) applyImport(replace = true)
                } else {
                    applyImport(replace = !repo.hasData())
                }
                _driveBackupsVisible.value = false
            } catch (e: DriveAuthException) {
                settings.setBackupGoogleEmail(null)
                _googleEmail.value = null
                _message.value = "Требуется вход в Google"
            } catch (e: Exception) {
                _message.value = e.message ?: "Не удалось импортировать с Диска"
            } finally {
                _busy.value = false
            }
        }
    }

    fun applyImport(replace: Boolean) {
        val file = _pending.value
        val products = _pendingProducts.value
        if (file == null && products.isEmpty()) return
        launch {
            _busy.value = true
            try {
                val parts = mutableListOf<String>()
                if (file != null) {
                    repo.importDays(file.days, replace)
                    parts.add("${file.days.size} ${plural(file.days.size)}")
                }
                if (products.isNotEmpty()) {
                    productRepository.importFood(products, replace)
                    parts.add("${products.size} ${productPlural(products.size)}")
                }
                _message.value = "Импортировано: ${parts.joinToString(", ")}"
            } catch (e: Exception) {
                _message.value = e.message ?: "Не удалось импортировать"
            } finally {
                _pending.value = null
                _pendingProducts.value = emptyList()
                _needStrategy.value = false
                _busy.value = false
            }
        }
    }

    fun cancelImport() {
        _pending.value = null
        _pendingProducts.value = emptyList()
        _needStrategy.value = false
    }

    fun requestAuthorization(afterSuccess: (suspend () -> Unit)? = null) {
        pendingAction = afterSuccess
        launch {
            _busy.value = true
            try {
                when (val outcome = authFlow.authorize()) {
                    is GoogleAuthFlow.Outcome.Success -> finishAuthorization(outcome.email)
                    is GoogleAuthFlow.Outcome.NeedsResolution ->
                        _authResolution.value = outcome.sender
                    is GoogleAuthFlow.Outcome.Failure -> {
                        pendingAction = null
                        _message.value = outcome.message
                    }
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun onAuthorizationResolutionDone(resultCode: Int? = null, data: Intent? = null) {
        _authResolution.value = null
        launch {
            _busy.value = true
            try {
                when (val outcome = authFlow.completeResolution(resultCode, data)) {
                    is GoogleAuthFlow.Outcome.Success -> finishAuthorization(outcome.email)
                    is GoogleAuthFlow.Outcome.NeedsResolution -> {
                        pendingAction = null
                        _message.value = "Доступ к Google Диску не предоставлен"
                    }
                    is GoogleAuthFlow.Outcome.Failure -> {
                        pendingAction = null
                        _message.value = outcome.message
                    }
                }
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun finishAuthorization(email: String?) {
        val displayEmail = authFlow.applySuccess(email)
        _googleEmail.value = displayEmail
        _message.value = "Google Диск подключён"
        val action = pendingAction
        pendingAction = null
        action?.invoke()
    }

    private fun plural(n: Int): String = when {
        n % 10 == 1 && n % 100 != 11 -> "день"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "дня"
        else -> "дней"
    }

    private fun productPlural(n: Int): String = when {
        n % 10 == 1 && n % 100 != 11 -> "продукт"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "продукта"
        else -> "продуктов"
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                ImportExportViewModel(
                    repo = container.dayRepository,
                    productRepository = container.productRepository,
                    appContext = container.appContext,
                    settings = container.settingsDataStore,
                    driveClient = container.driveClient,
                )
            }
        }
    }
}
