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

    @Query("SELECT * FROM day ORDER BY date")
    suspend fun getAll(): List<DayEntity>

    @Query("SELECT * FROM day WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun getRange(from: String, to: String): List<DayEntity>

    @Query("SELECT COUNT(*) FROM day")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: DayEntity)

    @Query("DELETE FROM day WHERE date = :date")
    suspend fun delete(date: String)

    @Query("DELETE FROM day")
    suspend fun deleteAll()
}
