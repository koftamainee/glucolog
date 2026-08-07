package com.koftamainee.glucolog.domain

import kotlin.math.roundToInt
import kotlin.math.sqrt

data class DayStats(
    val n: Int,
    val min: Float,
    val max: Float,
    val avg: Float,
    val sd: Float,
    val hypo: Int,
    val hyper: Int,
    val trend: Trend?,
) {
    enum class Trend { UP, DOWN, FLAT }
}

fun statsOf(points: List<Float>, prevAvg: Float?): DayStats? {
    if (points.isEmpty()) return null
    val n = points.size
    val min = points.min()
    val max = points.max()
    val avg = points.sum() / n
    val sd = sqrt(points.sumOf { ((it - avg) * (it - avg)).toDouble() } / n).toFloat()
    val hypo = countZones(points, 4f, above = false)
    val hyper = countZones(points, 10f, above = true)
    val trend = prevAvg?.let { prev ->
        val diff = avg - prev
        when {
            diff > 0.3f -> DayStats.Trend.UP
            diff < -0.3f -> DayStats.Trend.DOWN
            else -> DayStats.Trend.FLAT
        }
    }
    return DayStats(n, min, max, avg, sd, hypo, hyper, trend)
}

fun fmt1(v: Float): String = (v * 10).roundToInt().let { "${it / 10}.${it % 10}" }

private fun countZones(points: List<Float>, limit: Float, above: Boolean): Int {
    var zones = 0
    var inZone = false
    for (g in points) {
        val hit = if (above) g > limit else g < limit
        if (hit && !inZone) zones++
        inZone = hit
    }
    return zones
}
