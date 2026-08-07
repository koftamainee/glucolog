package com.koftamainee.glucolog.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StoolDao {
    @Query("SELECT * FROM stool WHERE date = :date ORDER BY option")
    fun observe(date: String): Flow<List<StoolEntity>>

    @Query("SELECT * FROM stool WHERE date = :date ORDER BY option")
    suspend fun get(date: String): List<StoolEntity>

    @Query("SELECT * FROM stool ORDER BY date, option")
    suspend fun getAll(): List<StoolEntity>

    @Query("SELECT * FROM stool WHERE date BETWEEN :from AND :to ORDER BY date, option")
    suspend fun getRange(from: String, to: String): List<StoolEntity>

    @Query("SELECT COUNT(*) FROM stool")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stool: StoolEntity)

    @Delete
    suspend fun delete(stool: StoolEntity)

    @Query("DELETE FROM stool")
    suspend fun deleteAll()
}
