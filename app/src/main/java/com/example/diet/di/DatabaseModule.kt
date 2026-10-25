package com.example.diet.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.diet.data.local.AppDatabase
import com.example.diet.data.local.dao.DrinkPresetDao
import com.example.diet.data.local.dao.MealDao
import com.example.diet.data.local.dao.MealItemDao
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

    /**
     * v5：新增冲调型水种（按每份干粉记录营养）与冲调记录字段；
     * 内置「蛋白粉」同步改为冲调型（一勺 30g）。
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE drink_preset ADD COLUMN kind TEXT NOT NULL DEFAULT 'LIQUID'")
            db.execSQL("ALTER TABLE drink_preset ADD COLUMN servingGrams REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE drink_preset ADD COLUMN caloriesPerServing REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE drink_preset ADD COLUMN proteinPerServing REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE drink_preset ADD COLUMN carbPerServing REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE drink_preset ADD COLUMN fatPerServing REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE water_record ADD COLUMN servings REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE water_record ADD COLUMN powderGrams REAL NOT NULL DEFAULT 0")
            db.execSQL(
                "UPDATE drink_preset SET kind = 'POWDER', servingGrams = 30, " +
                    "caloriesPerServing = 120, proteinPerServing = 24, " +
                    "carbPerServing = 3, fatPerServing = 1.5 " +
                    "WHERE builtIn = 1 AND name = '蛋白粉'"
            )
        }
    }

    /**
     * v6：新增数据状态字段，取代原来「靠 summary 文案推断数据是否缺失」的做法。
     * 历史数据按原有可判定信息回填：未分析且无热量 → 缺失；手动记录 → 已确认；
     * 摘要标记为缺失 → 缺失；摘要提示部分缺失 → 部分缺失；其余 → 估算。
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE meal_record ADD COLUMN status TEXT NOT NULL DEFAULT 'ESTIMATED'")
            db.execSQL("ALTER TABLE water_record ADD COLUMN status TEXT NOT NULL DEFAULT 'CONFIRMED'")
            db.execSQL(
                "UPDATE meal_record SET status = CASE " +
                    "WHEN analyzedByAi = 0 AND caloriesKcal = 0 THEN 'MISSING' " +
                    "WHEN analyzedByAi = 0 THEN 'CONFIRMED' " +
                    "WHEN summary LIKE '⚠%' THEN 'MISSING' " +
                    "WHEN caloriesKcal = 0 THEN 'MISSING' " +
                    "WHEN summary LIKE '%缺少份量或营养数据%' THEN 'PARTIAL' " +
                    "ELSE 'ESTIMATED' END"
            )
        }
    }

    /**
     * v7：新增 meal_item 明细表。历史记录没有条目（详情页会说明"无明细"），
     * 记录头上的总量保持不变，作为该记录的汇总缓存。
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `meal_item` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`recordId` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`brand` TEXT NOT NULL, " +
                    "`grams` REAL NOT NULL, " +
                    "`kcalPer100g` REAL NOT NULL, " +
                    "`proteinPer100g` REAL NOT NULL, " +
                    "`carbPer100g` REAL NOT NULL, " +
                    "`fatPer100g` REAL NOT NULL, " +
                    "`source` TEXT NOT NULL, " +
                    "`confidence` REAL NOT NULL, " +
                    "`status` TEXT NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`recordId`) REFERENCES `meal_record`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_meal_item_recordId` ON `meal_item` (`recordId`)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "diet_manager.db")
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides fun provideWeightDao(db: AppDatabase): WeightDao = db.weightDao()

    @Provides fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()

    @Provides fun provideMealItemDao(db: AppDatabase): MealItemDao = db.mealItemDao()

    @Provides fun provideWaterDao(db: AppDatabase): WaterDao = db.waterDao()

    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides fun provideQuickItemDao(db: AppDatabase): QuickItemDao = db.quickItemDao()

    @Provides fun provideDrinkPresetDao(db: AppDatabase): DrinkPresetDao = db.drinkPresetDao()
}
