package com.example.diet.data.repository

import com.example.diet.data.local.dao.WeightDao
import com.example.diet.data.local.entity.WeightRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(
    private val dao: WeightDao
) {
    fun observeByDate(date: String): Flow<WeightRecord?> = dao.observeByDate(date)

    fun observeAll(): Flow<List<WeightRecord>> = dao.observeAll()

    suspend fun addWeight(record: WeightRecord): Long = dao.insert(record)

    suspend fun deleteWeight(record: WeightRecord) = dao.delete(record)
}
