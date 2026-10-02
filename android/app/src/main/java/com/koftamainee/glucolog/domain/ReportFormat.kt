package com.koftamainee.glucolog.domain

import java.time.LocalDate
import kotlin.math.roundToInt

fun fmtDateShort(key: String): String {
    val d = parseDateKey(key) ?: return key
    return "${d.dayOfMonth.toString().padStart(2, '0')}.${d.monthValue.toString().padStart(2, '0')}.${d.year}"
}

fun fmtDateDayMonth(key: String): String {
    val d = parseDateKey(key) ?: return key
    return "${d.dayOfMonth.toString().padStart(2, '0')}.${d.monthValue.toString().padStart(2, '0')}"
}

fun fmtDateWeekday(key: String): String {
    val d = parseDateKey(key) ?: return ""
    val names = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
    return names[d.dayOfWeek.value - 1]
}

fun parseDateKey(key: String): LocalDate? =
    runCatching { LocalDate.parse(key) }.getOrNull()

fun fmtG(value: Float?): String {
    if (value == null) return "—"
    return "${(value * 10f).roundToInt() / 10f}".replace('.', ',')
}

fun fmtG2(value: Float?): String {
    if (value == null) return "—"
    val scaled = (value * 100f).roundToInt()
    val whole = scaled / 100
    val frac = scaled % 100
    return "${whole.toString().replace('.', ',')},${frac.toString().padStart(2, '0')}"
}

fun fmtInt(value: Int?): String = value?.toString() ?: "—"

fun fmtPct(value: Float): String = "${value.roundToInt()}%"

fun fmtDurationMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "$h ч ${m.toString().padStart(2, '0')} мин"
}

fun pluralDays(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "день"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "дня"
    else -> "дней"
}

fun pluralEpisodes(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "эпизод"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "эпизода"
    else -> "эпизодов"
}

fun pluralReadings(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "измерение"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "измерения"
    else -> "измерений"
}

fun pluralMeals(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "приём"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "приёма"
    else -> "приёмов пищи"
}
