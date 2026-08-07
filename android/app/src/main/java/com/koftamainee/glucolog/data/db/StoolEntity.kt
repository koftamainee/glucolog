package com.koftamainee.glucolog.data.db

import androidx.room.Entity

@Entity(tableName = "stool", primaryKeys = ["date", "option"])
data class StoolEntity(
    val date: String,
    val option: String,
)
