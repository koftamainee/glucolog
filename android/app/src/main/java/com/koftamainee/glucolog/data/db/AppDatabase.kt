package com.koftamainee.glucolog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DayEntity::class,
        GlucoseEntity::class,
        InsulinEntity::class,
        MealEntity::class,
        StoolEntity::class,
    ],
    version = 2,
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `meal_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`date` TEXT NOT NULL, `key` TEXT NOT NULL, " +
                        "`time` TEXT, `hunger` INTEGER, `food` TEXT, `carbs` INTEGER)"
                )
                db.execSQL(
                    "INSERT INTO `meal_new` (`date`, `key`, `time`, `hunger`, `food`, `carbs`) " +
                        "SELECT `date`, `key`, `time`, `hunger`, `food`, NULL FROM `meal`"
                )
                db.execSQL("DROP TABLE `meal`")
                db.execSQL("ALTER TABLE `meal_new` RENAME TO `meal`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_meal_date_key` " +
                        "ON `meal` (`date`, `key`)"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun build(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
