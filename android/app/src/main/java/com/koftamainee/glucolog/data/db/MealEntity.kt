package com.koftamainee.glucolog.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal",
    indices = [Index(value = ["date", "key"], unique = true)],
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val key: String,
    val time: String? = null,
    val hunger: Int? = null,
    val food: String? = null,
    val phys: String? = null,
    val emo: String? = null,
)
