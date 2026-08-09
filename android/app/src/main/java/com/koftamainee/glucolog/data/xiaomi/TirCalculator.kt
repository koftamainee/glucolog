package com.koftamainee.glucolog.data.xiaomi

import com.koftamainee.glucolog.data.db.GlucoseEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

object TirCalculator {

    const val LOW_MMOL = 3.9f
    const val HIGH_MMOL = 10.0f
    private const val WINDOW_MS = 24L * 60L * 60L * 1000L

    data class Tir(val low: Int, val inRange: Int, val high: Int)

    fun calculate(readings: List<GlucoseEntity>, nowMs: Long = System.currentTimeMillis()): Tir {
        val since = nowMs - WINDOW_MS
        val zone = ZoneId.systemDefault()
        val recent = readings.filter { entity ->
            val t = entityEpochMillis(entity, zone)
            t in since..nowMs
        }
        if (recent.isEmpty()) return Tir(0, 0, 0)

        var lowCount = 0
        var inRangeCount = 0
        var highCount = 0
        for (e in recent) {
            when {
                e.g < LOW_MMOL -> lowCount++
                e.g > HIGH_MMOL -> highCount++
                else -> inRangeCount++
            }
        }
        val total = recent.size

        val inRangePercent = minutesToPercent(inRangeCount, total)
        var lowPercent = 0
        var highPercent = 0
        if (inRangePercent < 100) {
            val remainder = 100 - inRangePercent
            when {
                lowCount > 0 && highCount == 0 -> lowPercent = remainder
                highCount > 0 && lowCount == 0 -> highPercent = remainder
                lowCount > 0 && highCount > 0 -> {
                    if (highCount >= lowCount) {
                        highPercent = roundedRangePercent(highCount, total, inRangePercent)
                        lowPercent = remainder - highPercent
                    } else {
                        lowPercent = roundedRangePercent(lowCount, total, inRangePercent)
                        highPercent = remainder - lowPercent
                    }
                }
            }
        }
        return Tir(lowPercent, inRangePercent, highPercent)
    }

    private fun entityEpochMillis(e: GlucoseEntity, zone: ZoneId): Long {
        val date = LocalDate.parse(e.date)
        val hours = e.h.toInt()
        val minutes = round((e.h - hours) * 60).toInt()
        return LocalDateTime.of(date, LocalTime.of(hours, minutes))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    private fun minutesToPercent(minutes: Int, totalMinutes: Int): Int {
        if (minutes <= 0) return 0
        return minOf(100, ceil(100.0 * minutes / totalMinutes).toInt())
    }

    private fun roundedRangePercent(rangeMinutes: Int, totalMinutes: Int, inRangePercent: Int): Int {
        var percent = round(100.0 * rangeMinutes / totalMinutes).toInt()
        if (percent + inRangePercent >= 100) {
            percent = floor(100.0 * rangeMinutes / totalMinutes).toInt()
            if (percent + inRangePercent >= 100) {
                percent = 100 - inRangePercent
            }
        }
        return percent
    }
}
