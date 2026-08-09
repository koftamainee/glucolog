package com.koftamainee.glucolog.ui.xdrip

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koftamainee.glucolog.data.DayRepository
import com.koftamainee.glucolog.data.SettingsDataStore
import com.koftamainee.glucolog.data.xdrip.XdripStatus
import com.koftamainee.glucolog.data.xdrip.XdripStatusProvider
import com.koftamainee.glucolog.data.xdrip.XdripWebClient
import com.koftamainee.glucolog.data.xdrip.XdripBroadcast
import com.koftamainee.glucolog.data.xiaomi.XiaomiWatchService
import com.koftamainee.glucolog.di.AppContainer
import com.koftamainee.glucolog.domain.formatDateLabel
import com.koftamainee.glucolog.domain.floatToTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WatchStats(val lastSentMs: Long, val lastConfirmedMs: Long)

class XdripSetupViewModel(
    private val statusProvider: XdripStatusProvider,
    private val client: XdripWebClient,
    private val settings: SettingsDataStore,
    private val repo: DayRepository,
    private val appContext: Context,
) : ViewModel() {

    val status: StateFlow<XdripStatus> = statusProvider.status
        .stateIn(viewModelScope, SharingStarted.Eagerly, XdripStatus())

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking

    private val _checkMessage = MutableStateFlow<String?>(null)
    val checkMessage: StateFlow<String?> = _checkMessage

    val xiaomiEnabled: StateFlow<Boolean> = settings.xiaomiServiceEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val watchStats: StateFlow<WatchStats> = flow {
        while (true) {
            emit(WatchStats(XiaomiWatchService.lastSentMs, XiaomiWatchService.lastConfirmedMs))
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WatchStats(0, 0))

    private val _forceMessage = MutableStateFlow<String?>(null)
    val forceMessage: StateFlow<String?> = _forceMessage

    fun setXiaomiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setXiaomiServiceEnabled(enabled)
            if (enabled) {
                XiaomiWatchService.start(appContext)
            } else {
                XiaomiWatchService.stop(appContext)
            }
        }
    }

    fun forceX() {
        viewModelScope.launch {
            XdripBroadcast.register(appContext)
            _forceMessage.value = "Команда отправлена в xDrip"
        }
    }

    fun sync() {
        if (_checking.value) return
        _checking.value = true
        _checkMessage.value = null
        viewModelScope.launch {
            try {
                val readings = client.fetchSgv(count = SYNC_COUNT)
                if (readings.isEmpty()) {
                    settings.setXdripConnected(true)
                    _checkMessage.value = "Сервис отвечает, но чтений пока нет"
                } else {
                    repo.insertXdripReadings(readings)
                    settings.setXdripConnected(true)
                    val last = readings.last()
                    _checkMessage.value =
                        "Соединение установлено. Импортировано ${readings.size} показаний. " +
                            "Последнее: ${formatDateLabel(last.date)} ${floatToTime(last.h)} — " +
                            "%.1f ммоль/л".format(last.g)
                }
            } catch (e: Exception) {
                Log.e(TAG, "sync failed", e)
                _checkMessage.value =
                    "Нет соединения. Включите «Веб служба xDrip» в xDrip."
            } finally {
                _checking.value = false
            }
        }
    }

    companion object {
        private const val TAG = "XdripSetup"
        private const val SYNC_COUNT = 1000

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                XdripSetupViewModel(
                    statusProvider = container.xdripStatusProvider,
                    client = container.xdripWebClient,
                    settings = container.settingsDataStore,
                    repo = container.dayRepository,
                    appContext = container.appContext,
                )
            }
        }
    }
}
