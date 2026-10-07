package com.koftamainee.glucolog.ui.backup

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koftamainee.glucolog.data.DayRepository
import com.koftamainee.glucolog.data.ProductRepository
import com.koftamainee.glucolog.data.SettingsDataStore
import com.koftamainee.glucolog.data.backup.BackupScheduler
import com.koftamainee.glucolog.data.backup.DriveAuthException
import com.koftamainee.glucolog.data.backup.GoogleAuthFlow
import com.koftamainee.glucolog.data.backup.GoogleDriveClient
import com.koftamainee.glucolog.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BackupViewModel(
    private val repo: DayRepository,
    private val productRepository: ProductRepository,
    private val appContext: Context,
    private val settings: SettingsDataStore,
    private val backupScheduler: BackupScheduler,
    private val driveClient: GoogleDriveClient,
) : ViewModel() {

    private val authFlow = GoogleAuthFlow(appContext, settings)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _googleEmail = MutableStateFlow<String?>(null)
    val googleEmail: StateFlow<String?> = _googleEmail.asStateFlow()

    val backupEnabled: StateFlow<Boolean> = settings.backupEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val backupDays: StateFlow<Boolean> = settings.backupDays
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val backupProducts: StateFlow<Boolean> = settings.backupProducts
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val backupLastTime: StateFlow<Long> = settings.backupLastTime
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val backupLastError: StateFlow<String?> = settings.backupLastError
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _intervalText = MutableStateFlow("")
    val intervalText: StateFlow<String> = _intervalText.asStateFlow()

    private val _keepCountText = MutableStateFlow("")
    val keepCountText: StateFlow<String> = _keepCountText.asStateFlow()

    private val _authResolution = MutableStateFlow<IntentSender?>(null)
    val authResolution: StateFlow<IntentSender?> = _authResolution.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    private var pendingAction: (suspend () -> Unit)? = null

    init {
        viewModelScope.launch {
            _intervalText.value = settings.backupIntervalHours.first().toString()
            _keepCountText.value = settings.backupKeepCount.first().toString()
            _googleEmail.value = settings.backupGoogleEmail.first()
            verifyDriveLink()
        }
    }

    private suspend fun verifyDriveLink() {
        _checking.value = true
        try {
            val auth = GoogleDriveClient.authorizeSilently(appContext)
            val stored = _googleEmail.value
            if (stored == null || stored == FALLBACK_EMAIL) {
                val email = auth.email ?: GoogleDriveClient.resolveEmail(auth.accessToken)
                if (!email.isNullOrEmpty() && email != FALLBACK_EMAIL) {
                    settings.setBackupGoogleEmail(email)
                    _googleEmail.value = email
                }
            }
        } catch (e: DriveAuthException) {
            settings.setBackupGoogleEmail(null)
            _googleEmail.value = null
            if (settings.backupEnabled.first()) {
                settings.setBackupEnabled(false)
                backupScheduler.cancel()
                _message.value = "Доступ к Google Диску отозван — автобэкап выключен"
            }
        } catch (e: Exception) {
            Log.w(TAG, "verifyDriveLink failed", e)
        } finally {
            _checking.value = false
        }
    }

    fun setBackupEnabled(enabled: Boolean) {
        launch {
            settings.setBackupEnabled(enabled)
            if (enabled && _googleEmail.value == null) {
                requestAuthorization()
            } else {
                backupScheduler.rescheduleIfEnabled()
            }
        }
    }

    fun setIntervalText(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(5)
        _intervalText.value = digits
        val hours = digits.toIntOrNull()
            ?.coerceIn(1, BackupScheduler.MAX_INTERVAL_HOURS)
            ?: return
        launch {
            settings.setBackupIntervalHours(hours)
            backupScheduler.rescheduleIfEnabled()
        }
    }

    fun setKeepCountText(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(2)
        _keepCountText.value = digits
        val count = digits.toIntOrNull()
            ?.coerceIn(BackupScheduler.MIN_KEEP_COUNT, BackupScheduler.MAX_KEEP_COUNT)
            ?: return
        launch { settings.setBackupKeepCount(count) }
    }

    fun setBackupDays(value: Boolean) {
        launch { settings.setBackupDays(value) }
    }

    fun setBackupProducts(value: Boolean) {
        launch { settings.setBackupProducts(value) }
    }

    fun backupNow() {
        if (_googleEmail.value == null) {
            requestAuthorization {
                backupScheduler.runNow()
            }
        } else {
            _message.value = null
            backupScheduler.runNow()
        }
    }

    fun signOut() {
        launch {
            authFlow.signOut()
            backupScheduler.cancel()
            _googleEmail.value = null
            _message.value = "Выход выполнен — при следующем входе выберите аккаунт"
        }
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
        Log.d(TAG, "resolution done resultCode=$resultCode")
        _authResolution.value = null
        launch {
            _busy.value = true
            try {
                when (val outcome = authFlow.completeResolution(resultCode, data)) {
                    is GoogleAuthFlow.Outcome.Success -> finishAuthorization(outcome.email)
                    is GoogleAuthFlow.Outcome.NeedsResolution -> {
                        pendingAction = null
                        _message.value = if (resultCode == 0) {
                            "Авторизация Google отменена или не выполнена"
                        } else {
                            "Доступ к Google Диску не предоставлен"
                        }
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
        backupScheduler.rescheduleIfEnabled()
        val action = pendingAction
        pendingAction = null
        action?.invoke()
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        private const val TAG = "GlucologAuth"
        private const val FALLBACK_EMAIL = "аккаунт"

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                BackupViewModel(
                    repo = container.dayRepository,
                    productRepository = container.productRepository,
                    appContext = container.appContext,
                    settings = container.settingsDataStore,
                    backupScheduler = container.backupScheduler,
                    driveClient = container.driveClient,
                )
            }
        }
    }
}
