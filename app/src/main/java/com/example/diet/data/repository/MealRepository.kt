package com.example.diet.data.repository

import com.example.diet.data.local.dao.MealDao
import com.example.diet.data.local.entity.MealRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepository @Inject constructor(
    private val dao: MealDao
) {
    fun observeByDate(date: String): Flow<List<MealRecord>> = dao.observeByDate(date)

    fun observeCaloriesByDate(date: String): Flow<Int> = dao.observeCaloriesByDate(date)

    suspend fun addMeal(record: MealRecord): Long = dao.insert(record)

    suspend fun deleteMeal(record: MealRecord) = dao.delete(record)

    suspend fun updateMeal(record: MealRecord) = dao.update(record)
}
