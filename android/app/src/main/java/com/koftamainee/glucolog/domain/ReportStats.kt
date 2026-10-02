package com.koftamainee.glucolog.domain

import java.time.LocalDate
import kotlin.math.sqrt

object ReportStats {

    const val MMOL_TO_MGDL = 18.0182f

    const val VERY_LOW_CUT = 3.0f
    const val VERY_HIGH_CUT = 13.9f

    const val FASTING_FROM = 3f
    const val FASTING_TO = 8f

    const val PP_FROM = 1f
    const val PP_TO = 3f

    const val EPISODE_GAP_H = 1.25f
    const val NIGHT_FROM = 23f
    const val NIGHT_TO = 6f

    const val MIN_DAYS_FOR_VALID = 14
    const val MIN_COVERAGE = 0.7f

    const val BUCKET_H = 0.25f
}

fun mmolToMgdl(mmol: Float): Float = mmol * ReportStats.MMOL_TO_MGDL

fun hba1cFromMeanMmol(meanMmol: Float): Float {
    val mgdl = mmolToMgdl(meanMmol)
    return (mgdl + 46.7f) / 28.7f
}

fun gmiFromMeanMmol(meanMmol: Float): Float {
    val mgdl = mmolToMgdl(meanMmol)
    return 3.31f + 0.02392f * mgdl
}

fun hba1cPercentToMmolMol(percent: Float): Float = (percent - 2.15f) * 10.929f

fun bandPercent(
    readings: List<ReportReading>,
    lo: Float,
    hi: Float,
): List<RangeBand> {
    val total = readings.size
    fun pct(n: Int) = if (total == 0) 0f else n * 100f / total

    val veryLow = readings.count { it.g < ReportStats.VERY_LOW_CUT }
    val low = readings.count { it.g >= ReportStats.VERY_LOW_CUT && it.g < lo }
    val target = readings.count { it.g >= lo && it.g <= hi }
    val high = readings.count { it.g > hi && it.g <= ReportStats.VERY_HIGH_CUT }
    val veryHigh = readings.count { it.g > ReportStats.VERY_HIGH_CUT }

    return listOf(
        RangeBand(RangeBandKind.VERY_LOW, 0f, ReportStats.VERY_LOW_CUT, veryLow, pct(veryLow)),
        RangeBand(RangeBandKind.LOW, ReportStats.VERY_LOW_CUT, lo, low, pct(low)),
        RangeBand(RangeBandKind.TARGET, lo, hi, target, pct(target)),
        RangeBand(RangeBandKind.HIGH, hi, ReportStats.VERY_HIGH_CUT, high, pct(high)),
        RangeBand(RangeBandKind.VERY_HIGH, ReportStats.VERY_HIGH_CUT, 30f, veryHigh, pct(veryHigh)),
    )
}

fun isNightHour(h: Float): Boolean {
    val t = h % 24f
    return t >= ReportStats.NIGHT_FROM || t < ReportStats.NIGHT_TO
}

fun hypoEpisodes(
    readings: List<ReportReading>,
    lo: Float,
    gapHours: Float = ReportStats.EPISODE_GAP_H,
    absHourOf: (ReportReading) -> Long = { epochHour(it.date, it.h) },
): List<GlucoseEpisode> =
    detectEpisodes(
        readings = readings,
        hit = { it.g < lo },
        level2 = { it < ReportStats.VERY_LOW_CUT },
        fold = { current, g -> minOf(current, g) },
        gapHours = gapHours,
        absHourOf = absHourOf,
    )

fun hyperEpisodes(
    readings: List<ReportReading>,
    hi: Float,
    gapHours: Float = ReportStats.EPISODE_GAP_H,
    absHourOf: (ReportReading) -> Long = { epochHour(it.date, it.h) },
): List<GlucoseEpisode> =
    detectEpisodes(
        readings = readings,
        hit = { it.g > hi },
        level2 = { it > ReportStats.VERY_HIGH_CUT },
        fold = { current, g -> maxOf(current, g) },
        gapHours = gapHours,
        absHourOf = absHourOf,
    )

private fun detectEpisodes(
    readings: List<ReportReading>,
    hit: (ReportReading) -> Boolean,
    level2: (Float) -> Boolean,
    fold: (Float, Float) -> Float,
    gapHours: Float,
    absHourOf: (ReportReading) -> Long,
): List<GlucoseEpisode> {
    val out = mutableListOf<GlucoseEpisode>()
    val gapTicks = (gapHours / ReportStats.BUCKET_H).toLong()
    var startDate: String? = null
    var startH = 0f
    var endH = 0f
    var lastAbs = 0L
    var extremeG = 0f

    fun close() {
        val date = startDate ?: return
        out += GlucoseEpisode(
            date = date,
            startH = startH,
            endH = endH,
            minG = extremeG,
            night = isNightHour(startH),
            level2 = level2(extremeG),
        )
        startDate = null
    }

    readings.forEach { r ->
        if (hit(r)) {
            val abs = absHourOf(r)
            if (startDate != null && abs - lastAbs > gapTicks) close()
            if (startDate == null) {
                startDate = r.date
                startH = r.h
                extremeG = r.g
            }
            lastAbs = abs
            endH = r.h
            extremeG = fold(extremeG, r.g)
        } else {
            if (startDate != null) endH = r.h
            close()
        }
    }
    close()
    return out
}

fun buildReport(
    days: List<PortableDay>,
    from: LocalDate,
    to: LocalDate,
    targetLo: Float = 4f,
    targetHi: Float = 8f,
): ReportModel? {
    val lo = targetLo.coerceIn(
        ReportStats.VERY_LOW_CUT + 0.1f,
        ReportStats.VERY_HIGH_CUT - 0.5f,
    )
    val hi = targetHi.coerceIn(lo + 0.1f, ReportStats.VERY_HIGH_CUT - 0.1f)

    val readings = days.flatMap { day ->
        day.glucose.map { ReportReading(day.date, it.h, it.g, it.source) }
    }.sortedWith(compareBy({ it.date }, { it.h }))
    if (readings.isEmpty()) return null

    val periodDays = (to.toEpochDay() - from.toEpochDay()).toInt() + 1
    val requiredDays = minOf(periodDays, ReportStats.MIN_DAYS_FOR_VALID)
    val daysWithData = readings.map { it.date }.distinct().size
    val coverage = if (periodDays > 0) daysWithData.toFloat() / periodDays else 0f

    val values = FloatArray(readings.size) { readings[it].g }
    val mean = values.average().toFloat()
    val sd = if (values.size < 2) 0f else sqrt(values.sumOf { ((it - mean) * (it - mean)).toDouble() } / values.size).toFloat()
    val cv = if (mean > 0f) sd / mean * 100f else null

    val perDay = days.associateBy { it.date }
    val episodes = hypoEpisodes(readings, lo)
    val highEpisodes = hyperEpisodes(readings, hi)

    val dayReports = readings.groupBy { it.date }.toSortedMap().map { (date, list) ->
        val day = perDay[date]
        val dayValues = FloatArray(list.size) { list[it].g }
        val dayMean = dayValues.average().toFloat()
        val daySd = if (dayValues.size < 2) 0f
        else sqrt(dayValues.sumOf { ((it - dayMean) * (it - dayMean)).toDouble() } / dayValues.size).toFloat()
        DayReport(
            date = date,
            readings = list.size,
            meanG = dayMean,
            minG = dayValues.min(),
            maxG = dayValues.max(),
            sdG = daySd,
            basal = day?.insulin?.sumOf { (it.ba ?: 0f).toDouble() }?.toFloat() ?: 0f,
            bolus = day?.insulin?.sumOf { (it.b ?: 0f).toDouble() }?.toFloat() ?: 0f,
            carbs = day?.meals?.sumOf { it.carbs ?: 0 } ?: 0,
            hypoEpisodes = episodes.count { it.date == date },
            hyperEpisodes = countAbove(list.map { it.g }, hi),
        )
    }

    val byDate = readings.groupBy { it.date }

    val basalTotal = dayReports.sumOf { it.basal.toDouble() }.toFloat()
    val bolusTotal = dayReports.sumOf { it.bolus.toDouble() }.toFloat()
    val carbsTotal = dayReports.sumOf { it.carbs }
    val countedDays = daysWithData.coerceAtLeast(1)

    return ReportModel(
        from = DateKeys.key(from),
        to = DateKeys.key(to),
        periodDays = periodDays,
        daysWithData = daysWithData,
        coveragePercent = coverage * 100f,
        requiredDays = requiredDays,
        clinicallyValid = daysWithData >= requiredDays &&
            coverage >= ReportStats.MIN_COVERAGE,
        readings = readings.size,
        cgmReadings = readings.count { it.source == GlucoseSource.XDRIP.dbValue },
        manualReadings = readings.count { it.source == GlucoseSource.MANUAL.dbValue },
        readingsPerDay = readings.size.toFloat() / countedDays,
        meanG = mean,
        sdG = sd,
        cvPercent = cv,
        gmiPercent = gmiFromMeanMmol(mean),
        hba1cPercent = hba1cFromMeanMmol(mean),
        hba1cMmolMol = hba1cPercentToMmolMol(hba1cFromMeanMmol(mean)),
        targetLo = lo,
        targetHi = hi,
        bands = bandPercent(readings, lo, hi),
        fasting = fastingStat(readings),
        postprandial = postprandialStats(days, byDate),
        episodes = episodes,
        hypoCount = episodes.size,
        hypoLevel2Count = episodes.count { it.level2 },
        nightHypoCount = episodes.count { it.night },
        hyperEpisodes = highEpisodes,
        hyperCount = highEpisodes.size,
        nightHyperCount = highEpisodes.count { it.night },
        basalTotal = basalTotal,
        bolusTotal = bolusTotal,
        carbsTotal = carbsTotal,
        basalPerDay = basalTotal / countedDays,
        bolusPerDay = bolusTotal / countedDays,
        tddPerDay = (basalTotal + bolusTotal) / countedDays,
        carbsPerDay = carbsTotal.toFloat() / countedDays,
        bolusCarbRatio = if (carbsTotal > 0) bolusTotal / carbsTotal else null,
        days = dayReports,
    )
}

fun fastingStat(readings: List<ReportReading>): SlotStat? {
    val inWindow = readings.filter { it.h >= ReportStats.FASTING_FROM && it.h < ReportStats.FASTING_TO }
    if (inWindow.isEmpty()) return null
    return SlotStat(
        title = "Натощак",
        readings = inWindow.size,
        meanG = inWindow.map { it.g }.average().toFloat(),
        maxG = inWindow.maxOf { it.g },
    )
}

fun postprandialStats(
    days: List<PortableDay>,
    byDate: Map<String, List<ReportReading>>,
): SlotStat? {
    var n = 0
    var sum = 0.0
    var max: Float? = null
    days.forEach { day ->
        day.meals.forEach { meal ->
            val mh = timeToFloat(meal.time) ?: return@forEach
            val window = postprandialReadings(day, mh, byDate)
            if (window.isEmpty()) return@forEach
            n += window.size
            sum += window.sumOf { it.g.toDouble() }
            val dayMax = window.maxOf { it.g }
            max = maxOf(max ?: dayMax, dayMax)
        }
    }
    if (n == 0) return null
    return SlotStat(
        title = "После еды",
        readings = n,
        meanG = (sum / n).toFloat(),
        maxG = max,
    )
}

private fun postprandialReadings(
    day: PortableDay,
    mealHour: Float,
    byDate: Map<String, List<ReportReading>>,
): List<ReportReading> {
    val start = mealHour + ReportStats.PP_FROM
    val end = mealHour + ReportStats.PP_TO
    val out = mutableListOf<ReportReading>()

    fun collect(date: String, from: Float, to: Float) {
        byDate[date]?.forEach { r ->
            if (r.h >= from && r.h < to) out += r
        }
    }

    if (end <= 24f) {
        collect(day.date, start, end)
    } else {
        collect(day.date, start, 24f)
        val nextDate = runCatching {
            LocalDate.parse(day.date).plusDays(1).let { DateKeys.key(it) }
        }.getOrNull()
        if (nextDate != null) collect(nextDate, 0f, end - 24f)
    }
    return out
}

fun epochHour(date: String, h: Float): Long {
    val day = runCatching { LocalDate.parse(date).toEpochDay() }.getOrElse { return -1L }
    return day * 96L + (h / ReportStats.BUCKET_H).toLong()
}

fun countAbove(values: List<Float>, hi: Float): Int {
    var count = 0
    var wasAbove = false
    values.forEach { g ->
        val hit = g > hi
        if (hit && !wasAbove) count++
        wasAbove = hit
    }
    return count
}
