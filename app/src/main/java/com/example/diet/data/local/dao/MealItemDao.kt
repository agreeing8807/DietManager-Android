package com.example.diet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.diet.data.local.entity.MealItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MealItemDao {
    @Insert
    suspend fun insert(item: MealItem): Long

    @Insert
    suspend fun insertAll(items: List<MealItem>): List<Long>

    @Update
    suspend fun update(item: MealItem)

    @Delete
    suspend fun delete(item: MealItem)

    @Query("SELECT * FROM meal_item WHERE recordId = :recordId ORDER BY sortOrder ASC, id ASC")
    fun observeByRecord(recordId: Long): Flow<List<MealItem>>

    @Query("SELECT * FROM meal_item WHERE recordId = :recordId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByRecord(recordId: Long): List<MealItem>

    @Query("DELETE FROM meal_item WHERE recordId = :recordId")
    suspend fun deleteByRecord(recordId: Long)

    @Query("SELECT * FROM meal_item")
    suspend fun getAll(): List<MealItem>

    @Query("DELETE FROM meal_item")
    suspend fun clear()
}
