package com.koftamainee.glucolog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koftamainee.glucolog.data.MarkerLineSettings
import com.koftamainee.glucolog.data.SettingsDataStore
import com.koftamainee.glucolog.data.TargetRangeSettings
import com.koftamainee.glucolog.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChartSettingsViewModel(
    private val settings: SettingsDataStore,
) : ViewModel() {

    val markerLines: StateFlow<MarkerLineSettings> = settings.markerLines
        .stateIn(viewModelScope, SharingStarted.Eagerly, MarkerLineSettings())

    val targetRange: StateFlow<TargetRangeSettings> = settings.targetRange
        .stateIn(viewModelScope, SharingStarted.Eagerly, TargetRangeSettings())

    fun setMarkerLineManual(value: Boolean) = launch { settings.setMarkerLineManual(value) }

    fun setMarkerLineMeal(value: Boolean) = launch { settings.setMarkerLineMeal(value) }

    fun setMarkerLineBolusStart(value: Boolean) = launch { settings.setMarkerLineBolusStart(value) }

    fun setMarkerLineBolusEnd(value: Boolean) = launch { settings.setMarkerLineBolusEnd(value) }

    fun setMarkerLineBasal(value: Boolean) = launch { settings.setMarkerLineBasal(value) }

    fun setTargetLo(value: Float) = launch { settings.setTargetLo(value) }

    fun setTargetHi(value: Float) = launch { settings.setTargetHi(value) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ChartSettingsViewModel(container.settingsDataStore)
            }
        }
    }
}
