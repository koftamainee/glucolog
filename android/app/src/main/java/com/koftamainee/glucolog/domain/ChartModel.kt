package com.koftamainee.glucolog.domain

// Temporary hack: meal icons render ~6 hours earlier than the DB time.
// Shift meal hours forward on the chart only; DB is not modified.
const val MEAL_CHART_SHIFT_HOURS = 6f

data class ChartPoint(val h: Float, val g: Float)

data class ChartMeal(val h: Float, val carbs: Int? = null)

data class ChartModel(
    val line: List<ChartPoint>,
    val manual: List<ChartPoint>,
    val meals: List<ChartMeal>,
    val bolus: List<ChartPoint>,
    val prevBolus: List<ChartPoint>,
    val basal: List<ChartPoint>,
) {
    companion object {
        val EMPTY = ChartModel(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        fun from(data: DayData): ChartModel {
            val line = data.glucose
                .filter { it.source == GlucoseSource.XDRIP.dbValue }
                .map { ChartPoint(it.h, it.g) }
                .sortedBy { it.h }
            val manual = data.glucose
                .filter { it.source == GlucoseSource.MANUAL.dbValue }
                .map { ChartPoint(it.h, it.g) }
                .sortedBy { it.h }
            val meals = data.meals
                .mapNotNull { m -> timeToFloat(m.time)?.let { ChartMeal(it + MEAL_CHART_SHIFT_HOURS, m.carbs) } }
                .sortedBy { it.h }
            val bolus = data.insulin
                .mapNotNull { p -> p.bolus?.takeIf { it > 0f }?.let { ChartPoint(p.h, it) } }
                .sortedBy { it.h }
            val prevBolus = data.prevInsulin
                .mapNotNull { p -> p.bolus?.takeIf { it > 0f }?.let { ChartPoint(p.h, it) } }
                .sortedBy { it.h }
            val basal = data.insulin
                .mapNotNull { p -> p.basal?.takeIf { it > 0f }?.let { ChartPoint(p.h, it) } }
                .sortedBy { it.h }
            return ChartModel(line, manual, meals, bolus, prevBolus, basal)
        }
    }
}
