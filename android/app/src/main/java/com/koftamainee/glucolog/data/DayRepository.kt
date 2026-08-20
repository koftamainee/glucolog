package com.koftamainee.glucolog.data

import com.koftamainee.glucolog.data.db.AppDatabase
import com.koftamainee.glucolog.data.db.DayEntity
import com.koftamainee.glucolog.data.db.GlucoseEntity
import com.koftamainee.glucolog.data.db.InsulinEntity
import com.koftamainee.glucolog.data.db.MealEntity
import com.koftamainee.glucolog.data.db.StoolEntity
import androidx.room.withTransaction
import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.domain.DayData
import com.koftamainee.glucolog.domain.DayTextField
import com.koftamainee.glucolog.domain.DateKeys
import com.koftamainee.glucolog.domain.GlucosePoint
import com.koftamainee.glucolog.domain.GlucoseSource
import com.koftamainee.glucolog.domain.InsulinPoint
import com.koftamainee.glucolog.domain.InsulinType
import com.koftamainee.glucolog.domain.MealEntry
import com.koftamainee.glucolog.domain.MealField
import com.koftamainee.glucolog.domain.PortableDay
import com.koftamainee.glucolog.data.xdrip.XdripReading
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
        val base = combine(
            dayDao.observe(key),
            glucoseDao.observe(key),
            insulinDao.observe(key),
            mealDao.observe(key),
            stoolDao.observe(key),
        ) { day, glucose, insulin, meals, stool ->
            DayData(date, day, glucose, insulin, emptyList(), meals, stool)
        }
        return combine(base, insulinDao.observe(DateKeys.key(date.minusDays(1)))) { data, prev ->
            data.copy(prevInsulin = prev)
        }
    }

    fun observePrevAvg(date: LocalDate): Flow<Float?> {
        val prev = date.minusDays(1)
        return glucoseDao.observeAvg(DateKeys.key(prev))
    }

    fun observeLastXdrip(): Flow<GlucoseEntity?> = glucoseDao.observeLastXdrip()

    suspend fun getDay(date: LocalDate): DayData {
        val key = DateKeys.key(date)
        return DayData(
            date = date,
            day = dayDao.get(key),
            glucose = glucoseDao.get(key),
            insulin = insulinDao.get(key),
            prevInsulin = insulinDao.get(DateKeys.key(date.minusDays(1))),
            meals = mealDao.getForDate(key),
            stool = stoolDao.get(key),
        )
    }

    suspend fun getGlucoseRange(from: LocalDate, to: LocalDate): List<GlucoseEntity> =
        glucoseDao.getRange(DateKeys.key(from), DateKeys.key(to))

    suspend fun getDaysRange(from: LocalDate, to: LocalDate): List<PortableDay> {
        val fromKey = DateKeys.key(from)
        val toKey = DateKeys.key(to)
        val dayMap = dayDao.getRange(fromKey, toKey).associateBy { it.date }
        val allGlucose = glucoseDao.getRange(fromKey, toKey)
        val allInsulin = insulinDao.getRange(fromKey, toKey)
        val allMeals = mealDao.getRange(fromKey, toKey)
        val allStool = stoolDao.getRange(fromKey, toKey)
        val dates = (dayMap.keys + allGlucose.map { it.date } + allInsulin.map { it.date } +
            allMeals.map { it.date } + allStool.map { it.date }).distinct().sorted()
        return dates.map { key ->
            val day = dayMap[key]
            PortableDay(
                date = key,
                glucose = allGlucose.filter { it.date == key }
                    .map { GlucosePoint(it.h, it.g, it.source) }
                    .sortedBy { it.h },
                insulin = allInsulin.filter { it.date == key }
                    .map { InsulinPoint(it.h, it.bolus, it.basal) }
                    .sortedBy { it.h },
                meals = allMeals.filter { it.date == key }
                    .map { MealEntry(it.key, it.time, it.hunger, it.food, it.carbs) },
                stool = allStool.filter { it.date == key }.map { it.option },
            )
        }
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

    suspend fun insertXdripReadings(readings: List<XdripReading>) {
        readings.forEach { r -> addGlucose(r.date, r.h, r.g, GlucoseSource.XDRIP) }
    }

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
        insulinDao.upsert(updated)
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
            MealField.CARBS -> current.copy(carbs = value?.toIntOrNull())
        }
        mealDao.upsert(updated)
    }

    suspend fun addMeal(date: LocalDate) {
        val key = DateKeys.key(date)
        val maxSortOrder = mealDao.getForDate(key).maxOfOrNull { it.sortOrder } ?: 0
        mealDao.insert(
            MealEntity(
                date = key,
                key = "custom_${System.nanoTime()}",
                sortOrder = maxSortOrder + 1,
            )
        )
    }

    suspend fun deleteMeal(id: Long) = mealDao.deleteById(id)

    suspend fun reorderMeals(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            mealDao.setSortOrder(id, index)
        }
    }

    suspend fun setMealSortOrder(date: LocalDate, mealKey: String, sortOrder: Int) {
        val key = DateKeys.key(date)
        val meal = mealDao.get(key, mealKey) ?: return
        mealDao.setSortOrder(meal.id, sortOrder)
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

    suspend fun allDays(): List<PortableDay> {
        val dayMap = dayDao.getAll().associateBy { it.date }
        val allGlucose = glucoseDao.getAll()
        val allInsulin = insulinDao.getAll()
        val allMeals = mealDao.getAll()
        val allStool = stoolDao.getAll()
        val dates = (dayMap.keys + allGlucose.map { it.date } + allInsulin.map { it.date } +
            allMeals.map { it.date } + allStool.map { it.date }).distinct().sorted()
        return dates.map { key ->
            val day = dayMap[key]
            PortableDay(
                date = key,
                glucose = allGlucose.filter { it.date == key }
                    .map { GlucosePoint(it.h, it.g, it.source) }
                    .sortedBy { it.h },
                insulin = allInsulin.filter { it.date == key }
                    .map { InsulinPoint(it.h, it.bolus, it.basal) }
                    .sortedBy { it.h },
                meals = allMeals.filter { it.date == key }
                    .map { MealEntry(it.key, it.time, it.hunger, it.food, it.carbs) },
                water = day?.water,
                sport = day?.sport,
                steps = day?.steps,
                sleepStart = day?.sleepStart,
                sleepEnd = day?.sleepEnd,
                stress = day?.stress,
                notes = day?.notes,
                conclusions = day?.conclusions,
                stool = allStool.filter { it.date == key }.map { it.option },
            )
        }
    }

    suspend fun hasData(): Boolean =
        dayDao.count() > 0 || glucoseDao.count() > 0 || insulinDao.count() > 0 ||
            mealDao.count() > 0 || stoolDao.count() > 0

    suspend fun importDays(days: List<PortableDay>, replace: Boolean) {
        db.withTransaction {
            val fileHasGlucose = days.any { it.glucose.isNotEmpty() }
            if (replace) {
                if (fileHasGlucose) glucoseDao.deleteAll()
                insulinDao.deleteAll()
                mealDao.deleteAll()
                dayDao.deleteAll()
                stoolDao.deleteAll()
            }
            days.forEach { day ->
                val key = day.date
                val existingGlucose = glucoseDao.get(key)
                day.glucose.forEach { p ->
                    val existing = existingGlucose.firstOrNull {
                        abs(it.h - p.h) < 0.001 && it.source == p.source
                    }
                    if (existing != null) {
                        if (abs(existing.g - p.g) > 0.001f) glucoseDao.update(existing.copy(g = p.g))
                    } else {
                        glucoseDao.insert(
                            GlucoseEntity(date = key, h = p.h, g = p.g, source = p.source)
                        )
                    }
                }
                val existingInsulin = insulinDao.get(key)
                day.insulin.forEach { p ->
                    val existing = existingInsulin.firstOrNull { abs(it.h - p.h) < 0.001 }
                    val updated = if (existing != null) {
                        existing.copy(bolus = p.b ?: existing.bolus, basal = p.ba ?: existing.basal)
                    } else {
                        InsulinEntity(date = key, h = p.h, bolus = p.b, basal = p.ba)
                    }
                    if ((updated.bolus ?: 0f) > 0f || (updated.basal ?: 0f) > 0f) {
                        insulinDao.upsert(updated)
                    }
                }
                day.meals.forEach { m ->
                    val existing = mealDao.get(key, m.key)
                    val updated = if (existing != null) {
                        existing.copy(
                            time = m.time ?: existing.time,
                            hunger = m.hunger ?: existing.hunger,
                            food = m.food ?: existing.food,
                            carbs = m.carbs ?: existing.carbs,
                        )
                    } else {
                        MealEntity(
                            date = key,
                            key = m.key,
                            time = m.time,
                            hunger = m.hunger,
                            food = m.food,
                            carbs = m.carbs,
                        )
                    }
                    mealDao.upsert(updated)
                }
                val hasScalars = day.water != null || day.sport != null || day.steps != null ||
                    day.sleepStart != null || day.sleepEnd != null || day.stress != null ||
                    day.notes != null || day.conclusions != null
                if (hasScalars) {
                    val current = dayDao.get(key) ?: DayEntity(date = key)
                    dayDao.upsert(
                        current.copy(
                            water = day.water ?: current.water,
                            sport = day.sport ?: current.sport,
                            steps = day.steps ?: current.steps,
                            sleepStart = day.sleepStart ?: current.sleepStart,
                            sleepEnd = day.sleepEnd ?: current.sleepEnd,
                            stress = day.stress ?: current.stress,
                            notes = day.notes ?: current.notes,
                            conclusions = day.conclusions ?: current.conclusions,
                        )
                    )
                }
                day.stool.forEach { option ->
                    stoolDao.insert(StoolEntity(date = key, option = option))
                }
            }
        }
    }
}
