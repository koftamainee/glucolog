package com.koftamainee.glucolog.domain

import kotlin.math.roundToInt

data class ReportReading(
    val date: String,
    val h: Float,
    val g: Float,
    val source: String,
)

enum class RangeBandKind(val title: String) {
    VERY_LOW("Очень низкая"),
    LOW("Низкая"),
    TARGET("В целевом диапазоне"),
    HIGH("Высокая"),
    VERY_HIGH("Очень высокая"),
}

data class RangeBand(
    val kind: RangeBandKind,
    val lo: Float,
    val hi: Float,
    val readings: Int,
    val percent: Float,
) {
    val perDayMinutes: Int get() = (percent / 100f * 24f * 60f).roundToInt()
}

data class SlotStat(
    val title: String,
    val readings: Int,
    val meanG: Float?,
    val maxG: Float?,
)

data class GlucoseEpisode(
    val date: String,
    val startH: Float,
    val endH: Float,
    val minG: Float,
    val night: Boolean,
    val level2: Boolean,
) {
    val startTime: String get() = floatToTime(startH % 24f)
    val endTime: String get() = floatToTime(endH % 24f)
}

data class DayReport(
    val date: String,
    val readings: Int,
    val meanG: Float?,
    val minG: Float?,
    val maxG: Float?,
    val sdG: Float?,
    val basal: Float,
    val bolus: Float,
    val carbs: Int,
    val hypoEpisodes: Int,
    val hyperEpisodes: Int,
) {
    val tdd: Float get() = basal + bolus
}

data class ReportModel(
    val from: String,
    val to: String,
    val periodDays: Int,
    val daysWithData: Int,
    val coveragePercent: Float,
    val requiredDays: Int,
    val clinicallyValid: Boolean,
    val readings: Int,
    val cgmReadings: Int,
    val manualReadings: Int,
    val readingsPerDay: Float,
    val meanG: Float?,
    val sdG: Float?,
    val cvPercent: Float?,
    val gmiPercent: Float?,
    val hba1cPercent: Float?,
    val hba1cMmolMol: Float?,
    val targetLo: Float,
    val targetHi: Float,
    val bands: List<RangeBand>,
    val fasting: SlotStat?,
    val postprandial: SlotStat?,
    val episodes: List<GlucoseEpisode>,
    val hypoCount: Int,
    val hypoLevel2Count: Int,
    val nightHypoCount: Int,
    val hyperEpisodes: List<GlucoseEpisode>,
    val hyperCount: Int,
    val nightHyperCount: Int,
    val basalTotal: Float,
    val bolusTotal: Float,
    val carbsTotal: Int,
    val basalPerDay: Float,
    val bolusPerDay: Float,
    val tddPerDay: Float,
    val carbsPerDay: Float,
    val bolusCarbRatio: Float?,
    val days: List<DayReport>,
) {
    val inRangePercent: Float
        get() = bands.firstOrNull { it.kind == RangeBandKind.TARGET }?.percent ?: 0f

    val lowPercent: Float
        get() = bands.firstOrNull { it.kind == RangeBandKind.LOW }?.percent ?: 0f

    val belowPercent: Float
        get() = bands.filter {
            it.kind == RangeBandKind.LOW || it.kind == RangeBandKind.VERY_LOW
        }.sumOf { it.percent.toDouble() }.toFloat()

    val abovePercent: Float
        get() = bands.filter {
            it.kind == RangeBandKind.HIGH || it.kind == RangeBandKind.VERY_HIGH
        }.sumOf { it.percent.toDouble() }.toFloat()
}
