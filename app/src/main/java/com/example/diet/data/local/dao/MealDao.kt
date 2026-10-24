package com.example.diet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.ColumnInfo
import androidx.room.Query
import androidx.room.Update
import com.example.diet.data.local.entity.MealRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert
    suspend fun insert(record: MealRecord): Long

    @Delete
    suspend fun delete(record: MealRecord)

    @Update
    suspend fun update(record: MealRecord)

    @Query("SELECT * FROM meal_record WHERE date = :date ORDER BY type ASC, createdAt ASC")
    fun observeByDate(date: String): Flow<List<MealRecord>>

    @Query("SELECT COALESCE(SUM(caloriesKcal),0) FROM meal_record WHERE date = :date")
    fun observeCaloriesByDate(date: String): Flow<Int>

    @Query(
        "SELECT COALESCE(SUM(proteinG), 0) AS proteinG, " +
        "COALESCE(SUM(carbG), 0) AS carbG, " +
        "COALESCE(SUM(fatG), 0) AS fatG " +
        "FROM meal_record WHERE date = :date"
    )
    suspend fun getMacrosByDate(date: String): MealMacros

    @Query("DELETE FROM meal_record")
    suspend fun clear()

    @Query("SELECT * FROM meal_record")
    suspend fun getAll(): List<MealRecord>
}

/** 单日三大营养素汇总（用于 Room 查询结果映射，不能直接返回 Triple） */
data class MealMacros(
    @ColumnInfo(name = "proteinG") val proteinG: Float,
    @ColumnInfo(name = "carbG") val carbG: Float,
    @ColumnInfo(name = "fatG") val fatG: Float
)
