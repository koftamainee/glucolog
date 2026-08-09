package com.koftamainee.glucolog.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product",
    indices = [Index(value = ["nameLower"])],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcal: Float,
    val proteins: Float,
    val fats: Float,
    val carbs: Float,
    val portionMass: Int,
    val note: String? = null,
    val source: String,
    @ColumnInfo(defaultValue = "") val nameLower: String = "",
    @ColumnInfo(defaultValue = "0") val hidden: Boolean = false,
    val lastUsed: Long? = null,
)
