package com.koftamainee.glucolog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {
    @Query("SELECT * FROM day WHERE date = :date")
    fun observe(date: String): Flow<DayEntity?>

    @Query("SELECT * FROM day WHERE date = :date")
    suspend fun get(date: String): DayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: DayEntity)

    @Query("DELETE FROM day WHERE date = :date")
    suspend fun delete(date: String)
}
