package com.example.diet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.diet.data.local.entity.MealRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert
    suspend fun insert(record: MealRecord): Long

    @Delete
    suspend fun delete(record: MealRecord)

    @Query("SELECT * FROM meal_record WHERE date = :date ORDER BY type ASC, createdAt ASC")
    fun observeByDate(date: String): Flow<List<MealRecord>>

    @Query("SELECT COALESCE(SUM(caloriesKcal),0) FROM meal_record WHERE date = :date")
    fun observeCaloriesByDate(date: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(proteinG),0), COALESCE(SUM(carbG),0), COALESCE(SUM(fatG),0) FROM meal_record WHERE date = :date")
    suspend fun getMacrosByDate(date: String): Triple<Float, Float, Float>

    @Query("DELETE FROM meal_record")
    suspend fun clear()

    @Query("SELECT * FROM meal_record")
    suspend fun getAll(): List<MealRecord>
}
