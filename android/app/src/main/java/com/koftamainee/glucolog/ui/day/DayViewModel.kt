package com.koftamainee.glucolog.ui.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koftamainee.glucolog.di.AppContainer
import com.koftamainee.glucolog.data.DayRepository
import com.koftamainee.glucolog.data.db.GlucoseEntity
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.domain.DayTextField
import com.koftamainee.glucolog.domain.GlucoseSource
import com.koftamainee.glucolog.domain.InsulinType
import com.koftamainee.glucolog.domain.MealField
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModel(
    private val repo: DayRepository,
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date

    val dayData: StateFlow<DayData?> = _date
        .flatMapLatest { repo.observeDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val prevAvg: StateFlow<Float?> = _date
        .flatMapLatest { repo.observePrevAvg(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDate(date: LocalDate) {
        _date.value = date
    }

    fun today() = setDate(LocalDate.now())

    fun shift(delta: Long) = setDate(_date.value.plusDays(delta))


    fun setWater(value: Int) = launch { repo.setDayField(_date.value) { copy(water = value) } }

    fun setSport(value: Boolean) = launch { repo.setDayField(_date.value) { copy(sport = value) } }

    fun setSteps(value: String) = launch {
        repo.setDayField(_date.value) { copy(steps = value.trim().toIntOrNull()) }
    }

    fun setSleepStart(value: String) = launch { repo.setDayField(_date.value) { copy(sleepStart = value) } }

    fun setSleepEnd(value: String) = launch { repo.setDayField(_date.value) { copy(sleepEnd = value) } }

    fun setStress(value: String?) = launch { repo.setDayField(_date.value) { copy(stress = value) } }

    fun setText(field: DayTextField, value: String) = launch { repo.setText(_date.value, field, value) }


    fun addGlucose(h: Float, g: Float, source: GlucoseSource) = launch {
        repo.addGlucose(_date.value, h, g, source)
    }

    fun updateGlucose(point: GlucoseEntity) = launch { repo.updateGlucose(point) }

    fun deleteGlucose(id: Long) = launch { repo.deleteGlucose(id) }


    fun setBolus(h: Float, value: Float) = launch { repo.setBolus(_date.value, h, value) }

    fun setBasal(h: Float, value: Float) = launch { repo.setBasal(_date.value, h, value) }

    fun removeInsulin(h: Float, type: InsulinType) = launch { repo.removeInsulin(_date.value, h, type) }


    fun setMealField(mealKey: String, field: MealField, value: String?) = launch {
        repo.setMealField(_date.value, mealKey, field, value)
    }


    fun toggleStool(option: String) = launch { repo.toggleStool(_date.value, option) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                DayViewModel(container.dayRepository)
            }
        }
    }
}
