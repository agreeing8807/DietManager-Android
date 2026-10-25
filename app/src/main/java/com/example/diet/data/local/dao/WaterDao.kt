package com.example.diet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.diet.data.local.entity.WaterRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(record: WaterRecord): Long

    @Update
    suspend fun update(record: WaterRecord)

    @Delete
    suspend fun delete(record: WaterRecord)

    @Query("SELECT * FROM water_record WHERE date = :date ORDER BY recordedAt ASC")
    fun observeByDate(date: String): Flow<List<WaterRecord>>

    /** 往日明细：一次性查询，供用户展开后修改 */
    @Query("SELECT * FROM water_record WHERE date = :date ORDER BY recordedAt ASC")
    suspend fun getByDate(date: String): List<WaterRecord>

    /** 计入水分的总量（旧数据 waterMl=0 时按 volumeMl 兜底） */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN waterMl > 0 THEN waterMl ELSE volumeMl END),0) " +
        "FROM water_record WHERE date = :date"
    )
    fun observeVolumeByDate(date: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(caloriesKcal),0) FROM water_record WHERE date = :date")
    fun observeCaloriesByDate(date: String): Flow<Int>

    /** 区间内按天汇总（用于往日每日总饮水量） */
    @Query(
        "SELECT date AS date, " +
        "COALESCE(SUM(CASE WHEN waterMl > 0 THEN waterMl ELSE volumeMl END),0) AS volumeMl, " +
        "COALESCE(SUM(caloriesKcal),0) AS caloriesKcal " +
        "FROM water_record WHERE date BETWEEN :start AND :end " +
        "GROUP BY date ORDER BY date DESC"
    )
    fun observeDailyTotalsBetween(start: String, end: String): Flow<List<DailyVolume>>

    @Query("DELETE FROM water_record")
    suspend fun clear()

    @Query("SELECT * FROM water_record")
    suspend fun getAll(): List<WaterRecord>
}

/** 单日饮水汇总（Room 查询结果映射用） */
data class DailyVolume(
    val date: String,
    val volumeMl: Int,
    val caloriesKcal: Int = 0
)
