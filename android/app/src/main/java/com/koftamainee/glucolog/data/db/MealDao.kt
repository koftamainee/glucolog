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
    @Query("SELECT * FROM meal WHERE date = :date ORDER BY sortOrder, id")
    fun observe(date: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal WHERE date = :date ORDER BY sortOrder, id")
    suspend fun getForDate(date: String): List<MealEntity>

    @Query("SELECT * FROM meal WHERE date = :date AND key = :key")
    suspend fun get(date: String, key: String): MealEntity?

    @Query("SELECT * FROM meal ORDER BY date, id")
    suspend fun getAll(): List<MealEntity>

    @Query("SELECT * FROM meal WHERE date BETWEEN :from AND :to ORDER BY date, id")
    suspend fun getRange(from: String, to: String): List<MealEntity>

    @Query("SELECT COUNT(*) FROM meal")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(meal: MealEntity): Long

    @Update
    suspend fun update(meal: MealEntity)

    @Upsert
    suspend fun upsert(meal: MealEntity)

    @Query("DELETE FROM meal WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE meal SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int)

    @Query("DELETE FROM meal")
    suspend fun deleteAll()
}
