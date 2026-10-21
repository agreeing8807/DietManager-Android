package com.example.diet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.diet.data.local.entity.WaterRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(record: WaterRecord): Long

    @Delete
    suspend fun delete(record: WaterRecord)

    @Query("SELECT * FROM water_record WHERE date = :date ORDER BY recordedAt ASC")
    fun observeByDate(date: String): Flow<List<WaterRecord>>

    @Query("SELECT COALESCE(SUM(volumeMl),0) FROM water_record WHERE date = :date")
    fun observeVolumeByDate(date: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(caloriesKcal),0) FROM water_record WHERE date = :date")
    fun observeCaloriesByDate(date: String): Flow<Int>

    @Query("DELETE FROM water_record")
    suspend fun clear()

    @Query("SELECT * FROM water_record")
    suspend fun getAll(): List<WaterRecord>
}
