package com.example.diet.data.repository

import com.example.diet.data.remote.LLMManager
import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.domain.model.MealAnalysis
import com.example.diet.domain.model.MealAnalysisRequest
import com.example.diet.domain.model.FoodSuggestionRequest
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
        val req = MealAnalysisRequest(
            description = description,
            imageBytes = imageBytes,
            userContext = userContext()
        )
        return llmManager.getSelectedProvider().analyzeMeal(req)
    }

    suspend fun suggestFoods(remainingCalories: Int, date: String): Result<List<FoodSuggestion>> {
        val req = FoodSuggestionRequest(
            remainingCalories = remainingCalories.coerceAtLeast(0),
            context = userContext(),
            date = date
        )
        return llmManager.getSelectedProvider().suggestFoods(req)
    }
}
