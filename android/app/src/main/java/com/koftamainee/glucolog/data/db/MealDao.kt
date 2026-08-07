package com.koftamainee.glucolog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meal WHERE date = :date ORDER BY key")
    fun observe(date: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal WHERE date = :date AND key = :key")
    suspend fun get(date: String, key: String): MealEntity?

    @Query("SELECT * FROM meal ORDER BY date, key")
    suspend fun getAll(): List<MealEntity>

    @Query("SELECT COUNT(*) FROM meal")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(meal: MealEntity): Long

    @Update
    suspend fun update(meal: MealEntity)

    @Upsert
    suspend fun upsert(meal: MealEntity)

    @Query("DELETE FROM meal")
    suspend fun deleteAll()
}
