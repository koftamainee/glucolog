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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stool: StoolEntity)

    @Delete
    suspend fun delete(stool: StoolEntity)
}
