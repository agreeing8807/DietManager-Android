package com.example.diet.data.repository

import com.example.diet.data.local.dao.DailyVolume
import com.example.diet.data.local.dao.WaterDao
import com.example.diet.data.local.entity.WaterRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepository @Inject constructor(
    private val dao: WaterDao
) {
    fun observeByDate(date: String): Flow<List<WaterRecord>> = dao.observeByDate(date)

    suspend fun getByDate(date: String): List<WaterRecord> = dao.getByDate(date)

    fun observeVolumeByDate(date: String): Flow<Int> = dao.observeVolumeByDate(date)

    fun observeCaloriesByDate(date: String): Flow<Int> = dao.observeCaloriesByDate(date)

    /** 区间内按天汇总的饮水量（含往日，用于只读展示每天总量） */
    fun observeDailyTotalsBetween(start: String, end: String): Flow<List<DailyVolume>> =
        dao.observeDailyTotalsBetween(start, end)

    suspend fun addWater(record: WaterRecord): Long = dao.insert(record)

    suspend fun updateWater(record: WaterRecord) = dao.update(record)

    suspend fun deleteWater(record: WaterRecord) = dao.delete(record)
}
