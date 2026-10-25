package com.example.diet.data.repository

import com.example.diet.data.remote.LLMManager
import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.domain.model.MealAnalysis
import com.example.diet.domain.model.MealAnalysisItem
import com.example.diet.domain.model.MealAnalysisRequest
import com.example.diet.domain.model.FoodSuggestionRequest
import com.example.diet.util.DateUtils
import com.example.diet.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisRepository @Inject constructor(
    private val llmManager: LLMManager,
    private val profileRepository: ProfileRepository
) {
    private suspend fun userContext(): String {
        val p = profileRepository.getProfile()
        return "性别:${p.gender.name}, 年龄:${p.age}, 身高:${p.heightCm.toInt()}cm, " +
            "当前体重:${p.currentWeightKg}kg, 目标体重:${p.targetWeightKg}kg, " +
            "每日热量目标:${p.dailyCalorieGoal}kcal"
    }

    suspend fun analyzeMeal(description: String, imageBytes: ByteArray? = null): Result<MealAnalysis> {
        // 统一在出口压缩：只影响上传体积，不改动原始照片；解码耗时应离开主线程
        val compressed = imageBytes?.let { bytes ->
            withContext(Dispatchers.IO) { ImageCompressor.compress(bytes) }
        }
        val req = MealAnalysisRequest(
            description = description,
            imageBytes = compressed,
            userContext = userContext()
        )
        val provider = llmManager.getActiveProvider()
            ?: return Result.failure(IllegalStateException("请先在设置中添加并选择一个 API"))
        return provider.analyzeMeal(req)
    }

    /** 补全已有条目缺失的营养数据（不重新识别） */
    suspend fun fillNutrition(
        items: List<MealAnalysisItem>
    ): Result<List<MealAnalysisItem>> {
        val provider = llmManager.getActiveProvider()
            ?: return Result.failure(IllegalStateException("请先在设置中添加并选择一个 API"))
        return provider.fillNutrition(items)
    }

    suspend fun suggestFoods(remainingCalories: Int, date: String): Result<List<FoodSuggestion>> {
        val req = FoodSuggestionRequest(
            remainingCalories = remainingCalories.coerceAtLeast(0),
            context = userContext(),
            date = date,
            currentTime = DateUtils.nowLabel(),
            mealPeriod = DateUtils.mealPeriod()
        )
        val provider = llmManager.getActiveProvider()
            ?: return Result.failure(IllegalStateException("请先在设置中添加并选择一个 API"))
        return provider.suggestFoods(req)
    }
}
