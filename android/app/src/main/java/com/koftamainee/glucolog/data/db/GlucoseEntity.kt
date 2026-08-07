package com.koftamainee.glucolog.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "glucose",
    indices = [Index(value = ["date", "h", "source"], unique = true)],
)
data class GlucoseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val h: Float,
    val g: Float,
    val source: String,
)
