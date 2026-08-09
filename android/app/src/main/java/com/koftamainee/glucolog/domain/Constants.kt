package com.koftamainee.glucolog.domain

import java.time.LocalDate

object Constants {
    val STOOL_OPTS = listOf(
        "Натощак",
        "Утром, после воды",
        "После 1 завтрака",
        "После 2 завтрака",
        "После обеда",
        "После полдника",
        "После ужина",
        "Запор",
        "Диарея",
    )

    val STRESS_OPTS = listOf("Нет", "Да", "Хронический")

    const val WATER_GLASSES = 8
    val HUNGER_LEVELS = 1..5

    const val DEFAULT_SLEEP_START = "22:00"
    const val DEFAULT_SLEEP_END = "06:00"
    const val CURRENT_SOURCE = "manual"

    fun blankToNull(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }
}

object DateKeys {
    fun key(date: LocalDate): String =
        "${date.year}-${date.monthValue.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"
}
