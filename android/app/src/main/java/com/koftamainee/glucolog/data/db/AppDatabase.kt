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
        ProductEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun glucoseDao(): GlucoseDao
    abstract fun insulinDao(): InsulinDao
    abstract fun mealDao(): MealDao
    abstract fun stoolDao(): StoolDao
    abstract fun foodDao(): FoodDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `product` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `kcal` REAL NOT NULL, " +
                        "`proteins` REAL NOT NULL, `fats` REAL NOT NULL, `carbs` REAL NOT NULL, " +
                        "`portionMass` INTEGER NOT NULL, `note` TEXT, `source` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recipe` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recipe_ingredient` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`recipeId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, " +
                        "`mass` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recipe_ingredient_recipeId_productId` " +
                        "ON `recipe_ingredient` (`recipeId`, `productId`)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `recipe`")
                db.execSQL("DROP TABLE IF EXISTS `recipe_ingredient`")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `product` ADD COLUMN `nameLower` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `product` ADD COLUMN `hidden` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `product` SET `nameLower` = lower(`name`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_product_nameLower` " +
                        "ON `product` (`nameLower`)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `product` ADD COLUMN `lastUsed` INTEGER")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `meal` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `meal` SET `sortOrder` = `id`")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun build(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                    .also { instance = it }
            }
    }
}
