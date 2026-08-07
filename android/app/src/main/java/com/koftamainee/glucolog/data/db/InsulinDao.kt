package com.koftamainee.glucolog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InsulinDao {
    @Query("SELECT * FROM insulin WHERE date = :date ORDER BY h")
    fun observe(date: String): Flow<List<InsulinEntity>>

    @Query("SELECT * FROM insulin WHERE date = :date ORDER BY h")
    suspend fun get(date: String): List<InsulinEntity>

    @Query("SELECT * FROM insulin ORDER BY date, h")
    suspend fun getAll(): List<InsulinEntity>

    @Query("SELECT * FROM insulin WHERE date BETWEEN :from AND :to ORDER BY date, h")
    suspend fun getRange(from: String, to: String): List<InsulinEntity>

    @Query("SELECT COUNT(*) FROM insulin")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(point: InsulinEntity): Long

    @Update
    suspend fun update(point: InsulinEntity)

    @Upsert
    suspend fun upsert(point: InsulinEntity)

    @Query("DELETE FROM insulin WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM insulin")
    suspend fun deleteAll()
}
