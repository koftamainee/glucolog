package com.koftamainee.glucolog.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day")
data class DayEntity(
    @PrimaryKey val date: String,
    val water: Int? = null,
    val sport: Boolean? = null,
    val steps: Int? = null,
    val sleepStart: String? = null,
    val sleepEnd: String? = null,
    val stress: String? = null,
    val notes: String? = null,
    val conclusions: String? = null,
)
