package com.koftamainee.glucolog.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "insulin",
    indices = [Index(value = ["date", "h"], unique = true)],
)
data class InsulinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val h: Float,
    val bolus: Float? = null,
    val basal: Float? = null,
)
