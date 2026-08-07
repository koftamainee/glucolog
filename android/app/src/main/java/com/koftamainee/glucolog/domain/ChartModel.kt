package com.koftamainee.glucolog.domain

data class ChartPoint(val h: Float, val g: Float)

data class ChartModel(
    val line: List<ChartPoint>,
    val manual: List<ChartPoint>,
    val bolus: List<ChartPoint>,
    val basal: List<ChartPoint>,
) {
    companion object {
        val EMPTY = ChartModel(emptyList(), emptyList(), emptyList(), emptyList())

        fun from(data: DayData): ChartModel {
            val line = data.glucose
                .filter { it.source == GlucoseSource.XDRIP.dbValue }
                .map { ChartPoint(it.h, it.g) }
                .sortedBy { it.h }
            val manual = data.glucose
                .filter { it.source == GlucoseSource.MANUAL.dbValue }
                .map { ChartPoint(it.h, it.g) }
                .sortedBy { it.h }
            val bolus = data.insulin
                .mapNotNull { p -> p.bolus?.takeIf { it > 0f }?.let { ChartPoint(p.h, it) } }
                .sortedBy { it.h }
            val basal = data.insulin
                .mapNotNull { p -> p.basal?.takeIf { it > 0f }?.let { ChartPoint(p.h, it) } }
                .sortedBy { it.h }
            return ChartModel(line, manual, bolus, basal)
        }
    }
}
