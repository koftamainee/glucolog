package com.koftamainee.glucolog.data.importexport

import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.domain.GlucosePoint
import com.koftamainee.glucolog.domain.InsulinPoint
import com.koftamainee.glucolog.domain.MealEntry
import com.koftamainee.glucolog.domain.MealField
import com.koftamainee.glucolog.domain.PortableDay
import kotlin.math.abs

internal class DayBuilder {
    private val glucose = mutableListOf<GlucosePoint>()
    private val insulin = mutableListOf<InsulinPoint>()
    private val meals = LinkedHashMap<String, MealEntry>()
    private val stool = mutableListOf<String>()
    var water: Int? = null
    var sport: Boolean? = null
    var steps: Int? = null
    var sleepStart: String? = null
    var sleepEnd: String? = null
    var stress: String? = null
    var notes: String? = null
    var conclusions: String? = null

    fun addGlucose(h: Float, g: Float, source: String) {
        if (g <= 0f || h !in 0f..24f) return
        val idx = glucose.indexOfFirst { abs(it.h - h) < 0.001f && it.source == source }
        if (idx >= 0) {
            glucose[idx] = GlucosePoint(h, g, source)
        } else {
            glucose.add(GlucosePoint(h, g, source))
        }
    }

    fun addInsulin(h: Float, b: Float?, ba: Float?) {
        if (b == null && ba == null) return
        val idx = insulin.indexOfFirst { abs(it.h - h) < 0.001f }
        if (idx >= 0) {
            val cur = insulin[idx]
            insulin[idx] = cur.copy(b = b ?: cur.b, ba = ba ?: cur.ba)
        } else {
            insulin.add(InsulinPoint(h, b, ba))
        }
    }

    fun putMeal(entry: MealEntry) {
        if (entry.key !in MEAL_KEYS) return
        meals[entry.key] = entry.copy(
            food = Constants.blankToNull(entry.food),
            phys = Constants.blankToNull(entry.phys),
            emo = Constants.blankToNull(entry.emo),
        )
    }

    fun setMealField(key: String, field: MealField, value: String?) {
        if (key !in MEAL_KEYS) return
        val current = meals[key] ?: MealEntry(key)
        meals[key] = when (field) {
            MealField.TIME -> current.copy(time = value)
            MealField.HUNGER -> current.copy(hunger = value?.toIntOrNull())
            MealField.FOOD -> current.copy(food = Constants.blankToNull(value))
            MealField.PHYS -> current.copy(phys = Constants.blankToNull(value))
            MealField.EMO -> current.copy(emo = Constants.blankToNull(value))
        }
    }

    fun addStool(option: String) {
        if (option.isNotEmpty() && option !in stool) stool.add(option)
    }

    fun toPortable(date: String): PortableDay {
        val orderedMeals = MEAL_KEYS.mapNotNull { meals[it] }
        return PortableDay(
            date = date,
            glucose = glucose.sortedBy { it.h },
            insulin = insulin.sortedBy { it.h },
            meals = orderedMeals,
            water = water,
            sport = sport,
            steps = steps,
            sleepStart = sleepStart,
            sleepEnd = sleepEnd,
            stress = stress,
            stool = stool,
            notes = Constants.blankToNull(notes),
            conclusions = Constants.blankToNull(conclusions),
        )
    }

    companion object {
        private val MEAL_KEYS = Constants.MEALS.map { it.key }
    }
}
