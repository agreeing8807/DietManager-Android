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

    /** 区间内按天汇总的饮水量（无记录的日期不会返回，需调用方补齐） */
    @Query(
        "SELECT date AS date, COALESCE(SUM(volumeMl),0) AS volumeMl " +
        "FROM water_record WHERE date BETWEEN :start AND :end GROUP BY date ORDER BY date ASC"
    )
    fun observeDailyVolumesBetween(start: String, end: String): Flow<List<DailyVolume>>

    @Query("DELETE FROM water_record")
    suspend fun clear()

    @Query("SELECT * FROM water_record")
    suspend fun getAll(): List<WaterRecord>
}

/** 单日饮水汇总（Room 查询结果映射用） */
data class DailyVolume(
    val date: String,
    val volumeMl: Int
)
