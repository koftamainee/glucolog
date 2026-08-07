package com.koftamainee.glucolog.domain

import java.time.LocalDate
import java.time.LocalTime

fun timeToFloat(time: String?): Float? {
    if (time.isNullOrBlank()) return null
    val parts = time.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h + m / 60f
}

fun floatToTime(hourFloat: Float): String {
    val h = hourFloat.toInt()
    val m = kotlin.math.round((hourFloat - h) * 60).toInt()
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}

fun currentTimeString(): String {
    val now = LocalTime.now()
    return "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
}

fun formatDateLabel(date: LocalDate): String {
    val days = listOf("вс", "пн", "вт", "ср", "чт", "пт", "сб")
    return "${days[date.dayOfWeek.value % 7]}, ${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthValue.toString().padStart(2, '0')}"
}

fun calcSleepDuration(start: String, end: String): String {
    val sh = start.split(":").firstOrNull()?.toIntOrNull() ?: 0
    val sm = start.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    val eh = end.split(":").firstOrNull()?.toIntOrNull() ?: 0
    val em = end.split(":").getOrNull(1)?.toIntOrNull() ?: 0

    var mins = (eh * 60 + em) - (sh * 60 + sm)
    if (mins < 0) mins += 1440

    val h = mins / 60
    val m = mins % 60

    return if (m != 0) "${h}ч ${m}м" else "${h}ч"
}
