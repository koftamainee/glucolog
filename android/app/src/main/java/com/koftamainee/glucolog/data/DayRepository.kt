package com.koftamainee.glucolog.data

import com.koftamainee.glucolog.data.db.AppDatabase
import com.koftamainee.glucolog.data.db.DayEntity
import com.koftamainee.glucolog.data.db.GlucoseEntity
import com.koftamainee.glucolog.data.db.InsulinEntity
import com.koftamainee.glucolog.data.db.MealEntity
import com.koftamainee.glucolog.data.db.StoolEntity
import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.domain.DayTextField
import com.koftamainee.glucolog.domain.DateKeys
import com.koftamainee.glucolog.domain.GlucoseSource
import com.koftamainee.glucolog.domain.InsulinType
import com.koftamainee.glucolog.domain.MealField
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DayRepository(private val db: AppDatabase) {

    private val dayDao = db.dayDao()
    private val glucoseDao = db.glucoseDao()
    private val insulinDao = db.insulinDao()
    private val mealDao = db.mealDao()
    private val stoolDao = db.stoolDao()

    fun observeDay(date: LocalDate): Flow<DayData> {
        val key = DateKeys.key(date)
        return combine(
            dayDao.observe(key),
            glucoseDao.observe(key),
            insulinDao.observe(key),
            mealDao.observe(key),
            stoolDao.observe(key),
        ) { day, glucose, insulin, meals, stool ->
            DayData(date, day, glucose, insulin, meals, stool)
        }
    }

    suspend fun getDay(date: LocalDate): DayData {
        val key = DateKeys.key(date)
        return DayData(
            date = date,
            day = dayDao.get(key),
            glucose = glucoseDao.get(key),
            insulin = insulinDao.get(key),
            meals = MEAL_KEYS.mapNotNull { keyOf -> mealDao.get(key, keyOf) },
            stool = stoolDao.get(key),
        )
    }

    suspend fun setDayField(date: LocalDate, setter: DayEntity.() -> DayEntity) {
        val key = DateKeys.key(date)
        val current = dayDao.get(key) ?: DayEntity(date = key)
        dayDao.upsert(setter(current))
    }

    suspend fun setText(date: LocalDate, field: DayTextField, value: String) {
        val clean = Constants.blankToNull(value)
        setDayField(date) { when (field) {
            DayTextField.NOTES -> copy(notes = clean)
            DayTextField.CONCLUSIONS -> copy(conclusions = clean)
        } }
    }

    suspend fun addGlucose(date: LocalDate, h: Float, g: Float, source: GlucoseSource) {
        val key = DateKeys.key(date)
        val existing = glucoseDao.get(key)
            .firstOrNull { abs(it.h - h) < 0.001 && it.source == source.dbValue }
        if (existing != null) {
            glucoseDao.update(existing.copy(g = g))
        } else {
            glucoseDao.insert(GlucoseEntity(date = key, h = h, g = g, source = source.dbValue))
        }
    }

    suspend fun updateGlucose(point: GlucoseEntity) = glucoseDao.update(point)

    suspend fun deleteGlucose(id: Long) = glucoseDao.deleteById(id)

    suspend fun setBolus(date: LocalDate, h: Float, value: Float) =
        setInsulinType(date, h, InsulinType.BOLUS, value)

    suspend fun setBasal(date: LocalDate, h: Float, value: Float) =
        setInsulinType(date, h, InsulinType.BASAL, value)

    private suspend fun setInsulinType(date: LocalDate, h: Float, type: InsulinType, value: Float) {
        val key = DateKeys.key(date)
        val existing = insulinDao.get(key).firstOrNull { abs(it.h - h) < 0.001 }
        val updated = if (existing != null) {
            when (type) {
                InsulinType.BOLUS -> existing.copy(bolus = value)
                InsulinType.BASAL -> existing.copy(basal = value)
            }
        } else {
            InsulinEntity(
                date = key,
                h = h,
                bolus = if (type == InsulinType.BOLUS) value else null,
                basal = if (type == InsulinType.BASAL) value else null,
            )
        }
        insulinDao.insert(updated)
    }

    suspend fun removeInsulin(date: LocalDate, h: Float, type: InsulinType) {
        val key = DateKeys.key(date)
        val existing = insulinDao.get(key).firstOrNull { abs(it.h - h) < 0.001 } ?: return
        val updated = when (type) {
            InsulinType.BOLUS -> existing.copy(bolus = null)
            InsulinType.BASAL -> existing.copy(basal = null)
        }
        if ((updated.bolus ?: 0f) <= 0f && (updated.basal ?: 0f) <= 0f) {
            insulinDao.deleteById(existing.id)
        } else {
            insulinDao.update(updated)
        }
    }

    suspend fun setMealField(date: LocalDate, mealKey: String, field: MealField, value: String?) {
        val key = DateKeys.key(date)
        val current = mealDao.get(key, mealKey)
            ?: MealEntity(date = key, key = mealKey)
        val updated = when (field) {
            MealField.TIME -> current.copy(time = value)
            MealField.HUNGER -> current.copy(hunger = value?.toIntOrNull())
            MealField.FOOD -> current.copy(food = Constants.blankToNull(value))
            MealField.PHYS -> current.copy(phys = Constants.blankToNull(value))
            MealField.EMO -> current.copy(emo = Constants.blankToNull(value))
        }
        mealDao.insert(updated)
    }

    suspend fun toggleStool(date: LocalDate, option: String) {
        val key = DateKeys.key(date)
        val current = stoolDao.get(key)
        if (current.any { it.option == option }) {
            stoolDao.delete(StoolEntity(date = key, option = option))
        } else {
            stoolDao.insert(StoolEntity(date = key, option = option))
        }
    }

    companion object {
        private val MEAL_KEYS = Constants.MEALS.map { it.key }
    }
}
