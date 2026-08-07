package com.koftamainee.glucolog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DayEntity::class,
        GlucoseEntity::class,
        InsulinEntity::class,
        MealEntity::class,
        StoolEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun glucoseDao(): GlucoseDao
    abstract fun insulinDao(): InsulinDao
    abstract fun mealDao(): MealDao
    abstract fun stoolDao(): StoolDao

    companion object {
        private const val NAME = "glucolog.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun build(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                    .build()
                    .also { instance = it }
            }
    }
}
