package com.example.diet.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.diet.data.local.AppDatabase
import com.example.diet.data.local.dao.DrinkPresetDao
import com.example.diet.data.local.dao.MealDao
import com.example.diet.data.local.dao.QuickItemDao
import com.example.diet.data.local.dao.ReminderDao
import com.example.diet.data.local.dao.UserProfileDao
import com.example.diet.data.local.dao.WaterDao
import com.example.diet.data.local.dao.WeightDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_profile ADD COLUMN birthday INTEGER")
        }
    }

    /** v3：新增快捷吃喝项、饮品配方；饮水记录补充水分/营养；资料补充每日开始时间 */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `quick_item` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`caloriesKcal` INTEGER NOT NULL, " +
                    "`waterMl` INTEGER NOT NULL, " +
                    "`proteinG` REAL NOT NULL, " +
                    "`carbG` REAL NOT NULL, " +
                    "`fatG` REAL NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `drink_preset` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`builtIn` INTEGER NOT NULL, " +
                    "`waterRatio` REAL NOT NULL, " +
                    "`caloriesPer100ml` REAL NOT NULL, " +
                    "`proteinPer100ml` REAL NOT NULL, " +
                    "`carbPer100ml` REAL NOT NULL, " +
                    "`fatPer100ml` REAL NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL)"
            )
            db.execSQL("ALTER TABLE water_record ADD COLUMN waterMl INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE water_record ADD COLUMN drinkName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE water_record ADD COLUMN proteinG REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE water_record ADD COLUMN carbG REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE water_record ADD COLUMN fatG REAL NOT NULL DEFAULT 0")
            db.execSQL("UPDATE water_record SET waterMl = volumeMl")
            db.execSQL("ALTER TABLE user_profile ADD COLUMN dayStartHour INTEGER NOT NULL DEFAULT 4")
        }
    }

    /** v4：加餐拆分为早/午/晚加餐，旧的 SNACK 归到晚加餐 */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE meal_record SET type = 'EVENING_SNACK' WHERE type = 'SNACK'")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "diet_manager.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides fun provideWeightDao(db: AppDatabase): WeightDao = db.weightDao()

    @Provides fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()

    @Provides fun provideWaterDao(db: AppDatabase): WaterDao = db.waterDao()

    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides fun provideQuickItemDao(db: AppDatabase): QuickItemDao = db.quickItemDao()

    @Provides fun provideDrinkPresetDao(db: AppDatabase): DrinkPresetDao = db.drinkPresetDao()
}
