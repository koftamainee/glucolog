package com.koftamainee.glucolog.data

import com.koftamainee.glucolog.data.db.AppDatabase
import com.koftamainee.glucolog.domain.DateKeys
import com.koftamainee.glucolog.domain.GlucosePoint
import com.koftamainee.glucolog.domain.InsulinPoint
import com.koftamainee.glucolog.domain.MealEntry
import com.koftamainee.glucolog.domain.PortableDay
import com.koftamainee.glucolog.domain.ReportModel
import com.koftamainee.glucolog.domain.buildReport
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class ReportRepository(
    private val database: AppDatabase,
    private val settings: SettingsDataStore,
) {

    suspend fun build(from: LocalDate, to: LocalDate): ReportModel? {
        val target = settings.targetRange.first()
        return buildReport(loadDays(from, to), from, to, target.lo, target.hi)
    }

    suspend fun loadDays(from: LocalDate, to: LocalDate): List<PortableDay> {
        val fromKey = DateKeys.key(from)
        val toKey = DateKeys.key(to)

        val glucoseByDate = database.glucoseDao().getRange(fromKey, toKey).groupBy { it.date }
        val insulinByDate = database.insulinDao().getRange(fromKey, toKey).groupBy { it.date }
        val mealsByDate = database.mealDao().getRange(fromKey, toKey).groupBy { it.date }
        val stoolByDate = database.stoolDao().getRange(fromKey, toKey).groupBy { it.date }
        val dayByDate = database.dayDao().getRange(fromKey, toKey).associateBy { it.date }

        // Grouping once per table keeps this linear in rows; DayRepository.getDaysRange
        // rescans the full result set per day and is too slow for 90-day reports.
        val keys = sortedSetOf<String>()
        keys.addAll(dayByDate.keys)
        keys.addAll(glucoseByDate.keys)
        keys.addAll(insulinByDate.keys)
        keys.addAll(mealsByDate.keys)
        keys.addAll(stoolByDate.keys)

        return keys.map { key ->
            val day = dayByDate[key]
            PortableDay(
                date = key,
                glucose = glucoseByDate[key].orEmpty()
                    .map { GlucosePoint(it.h, it.g, it.source) },
                insulin = insulinByDate[key].orEmpty()
                    .map { InsulinPoint(it.h, it.bolus, it.basal) },
                meals = mealsByDate[key].orEmpty()
                    .sortedWith(compareBy({ it.sortOrder }, { it.id }))
                    .map { MealEntry(it.key, it.time, it.hunger, it.food, it.carbs) },
                water = day?.water,
                sport = day?.sport,
                steps = day?.steps,
                sleepStart = day?.sleepStart,
                sleepEnd = day?.sleepEnd,
                stress = day?.stress,
                stool = stoolByDate[key].orEmpty().map { it.option },
                notes = day?.notes,
                conclusions = day?.conclusions,
            )
        }
    }
}
