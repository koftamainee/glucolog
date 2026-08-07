package com.koftamainee.glucolog.domain

data class PortableDay(
    val date: String,
    val glucose: List<GlucosePoint> = emptyList(),
    val insulin: List<InsulinPoint> = emptyList(),
    val meals: List<MealEntry> = emptyList(),
    val water: Int? = null,
    val sport: Boolean? = null,
    val steps: Int? = null,
    val sleepStart: String? = null,
    val sleepEnd: String? = null,
    val stress: String? = null,
    val stool: List<String> = emptyList(),
    val notes: String? = null,
    val conclusions: String? = null,
)

data class GlucosePoint(val h: Float, val g: Float, val source: String)

data class InsulinPoint(val h: Float, val b: Float? = null, val ba: Float? = null)

data class MealEntry(
    val key: String,
    val time: String? = null,
    val hunger: Int? = null,
    val food: String? = null,
    val carbs: Int? = null,
)
