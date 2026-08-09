package com.koftamainee.glucolog.ui.roam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koftamainee.glucolog.data.DayRepository
import com.koftamainee.glucolog.data.MarkerLineSettings
import com.koftamainee.glucolog.data.SettingsDataStore
import com.koftamainee.glucolog.di.AppContainer
import com.koftamainee.glucolog.domain.RoamModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val CONTEXT_DAYS = 1L
private const val INITIAL_RANGE_DAYS = 10L
private const val EXPAND_DAYS = 7L
private const val EDGE_DAYS = 2L

class RoamViewModel(
    savedStateHandle: SavedStateHandle,
    private val repo: DayRepository,
    private val settings: SettingsDataStore,
) : ViewModel() {

    private val openedDate: LocalDate =
        savedStateHandle.get<String>("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()

    val openedCenterHour: Float = openedDate.toEpochDay() * 24f + 12f

    private val _model = MutableStateFlow<RoamModel>(RoamModel.EMPTY)
    val model: StateFlow<RoamModel> = _model

    val markerLines: StateFlow<MarkerLineSettings> = settings.markerLines
        .stateIn(viewModelScope, SharingStarted.Eagerly, MarkerLineSettings())

    private var windowStart: LocalDate = openedDate.minusDays(INITIAL_RANGE_DAYS)
    private var windowEnd: LocalDate = openedDate.plusDays(INITIAL_RANGE_DAYS)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val from = windowStart.minusDays(CONTEXT_DAYS)
            _model.value = RoamModel.from(repo.getDaysRange(from, windowEnd))
        }
    }

    fun onViewportChanged(centerHour: Float) {
        val model = _model.value
        if (model === RoamModel.EMPTY) return
        val centerDay = LocalDate.ofEpochDay((centerHour / 24f).toLong())
        if (centerDay.isBefore(windowStart.plusDays(EDGE_DAYS))) {
            val newStart = windowStart.minusDays(EXPAND_DAYS)
            if (newStart.isBefore(windowStart)) {
                windowStart = newStart
                refresh()
            }
        } else if (centerDay.isAfter(windowEnd.minusDays(EDGE_DAYS))) {
            val newEnd = windowEnd.plusDays(EXPAND_DAYS)
            if (newEnd.isAfter(windowEnd)) {
                windowEnd = newEnd
                refresh()
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RoamViewModel(
                    createSavedStateHandle(),
                    container.dayRepository,
                    container.settingsDataStore,
                )
            }
        }
    }
}
