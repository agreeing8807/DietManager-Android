package com.example.diet.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.diet.data.local.AppDatabase
import com.example.diet.data.local.dao.MealDao
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "diet_manager.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides fun provideWeightDao(db: AppDatabase): WeightDao = db.weightDao()

    @Provides fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()

    @Provides fun provideWaterDao(db: AppDatabase): WaterDao = db.waterDao()

    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
}
