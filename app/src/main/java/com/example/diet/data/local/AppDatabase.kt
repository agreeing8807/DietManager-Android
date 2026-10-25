package com.example.diet.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.diet.data.local.dao.DrinkPresetDao
import com.example.diet.data.local.dao.MealDao
import com.example.diet.data.local.dao.MealItemDao
import com.example.diet.data.local.dao.QuickItemDao
import com.example.diet.data.local.dao.ReminderDao
import com.example.diet.data.local.dao.UserProfileDao
import com.example.diet.data.local.dao.WaterDao
import com.example.diet.data.local.dao.WeightDao
import com.example.diet.data.local.entity.DrinkPreset
import com.example.diet.data.local.entity.MealItem
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.local.entity.QuickItem
import com.example.diet.data.local.entity.ReminderSetting
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.data.local.entity.WaterRecord
import com.example.diet.data.local.entity.WeightRecord

@Database(
    entities = [
        UserProfile::class,
        WeightRecord::class,
        MealRecord::class,
        WaterRecord::class,
        ReminderSetting::class,
        QuickItem::class,
        DrinkPreset::class,
        MealItem::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun mealDao(): MealDao
    abstract fun mealItemDao(): MealItemDao
    abstract fun waterDao(): WaterDao
    abstract fun reminderDao(): ReminderDao
    abstract fun quickItemDao(): QuickItemDao
    abstract fun drinkPresetDao(): DrinkPresetDao
}
