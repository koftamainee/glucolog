package com.koftamainee.glucolog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {
    @Query("SELECT * FROM glucose WHERE date = :date ORDER BY h")
    fun observe(date: String): Flow<List<GlucoseEntity>>

    @Query("SELECT * FROM glucose WHERE date = :date ORDER BY h")
    suspend fun get(date: String): List<GlucoseEntity>

    @Query("SELECT * FROM glucose ORDER BY date, h")
    suspend fun getAll(): List<GlucoseEntity>

    @Query("SELECT AVG(g) FROM glucose WHERE date = :date")
    fun observeAvg(date: String): Flow<Float?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(point: GlucoseEntity): Long

    @Update
    suspend fun update(point: GlucoseEntity)

    @Query("DELETE FROM glucose WHERE id = :id")
    suspend fun deleteById(id: Long)
}
