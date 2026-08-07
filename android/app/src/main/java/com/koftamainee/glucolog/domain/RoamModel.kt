package com.koftamainee.glucolog.domain

import java.time.LocalDate

data class RoamModel(
    val days: List<LocalDate>,
    val line: List<ChartPoint>,
    val manual: List<ChartPoint>,
    val meals: List<ChartMeal>,
    val bolus: List<ChartPoint>,
    val basal: List<ChartPoint>,
    val startHour: Float,
    val endHour: Float,
) {
    companion object {
        val EMPTY = RoamModel(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 0f, 0f)

        fun from(days: List<PortableDay>): RoamModel {
            if (days.isEmpty()) return EMPTY
            val dates = days.map { LocalDate.parse(it.date) }.sorted()
            fun hour(d: LocalDate, h: Float): Float = d.toEpochDay() * 24f + h

            val line = mutableListOf<ChartPoint>()
            val manual = mutableListOf<ChartPoint>()
            val meals = mutableListOf<ChartMeal>()
            val bolus = mutableListOf<ChartPoint>()
            val basal = mutableListOf<ChartPoint>()

            days.forEach { day ->
                val date = LocalDate.parse(day.date)
                day.glucose.forEach { p ->
                    val point = ChartPoint(hour(date, p.h), p.g)
                    if (p.source == GlucoseSource.XDRIP.dbValue) line += point else manual += point
                }
                day.meals.forEach { m ->
                    m.time?.let { t ->
                        timeToFloat(t)?.let { hf ->
                            meals += ChartMeal(hour(date, hf) + MEAL_CHART_SHIFT_HOURS, m.carbs)
                        }
                    }
                }
                day.insulin.forEach { p ->
                    p.b?.takeIf { it > 0f }?.let { bolus += ChartPoint(hour(date, p.h), it) }
                    p.ba?.takeIf { it > 0f }?.let { basal += ChartPoint(hour(date, p.h), it) }
                }
            }

            return RoamModel(
                days = dates,
                line = line.sortedBy { it.h },
                manual = manual.sortedBy { it.h },
                meals = meals.sortedBy { it.h },
                bolus = bolus.sortedBy { it.h },
                basal = basal.sortedBy { it.h },
                startHour = dates.first().toEpochDay() * 24f,
                endHour = (dates.last().toEpochDay() + 1) * 24f,
            )
        }
    }
}
