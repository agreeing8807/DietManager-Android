package com.example.diet.data.remote

import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.domain.model.FoodSuggestionRequest
import com.example.diet.domain.model.MealAnalysis
import com.example.diet.domain.model.MealAnalysisRequest

/**
 * 统一大模型接入接口。不同厂商通过配置实现可切换，
 * 新增厂商只需在 LLMManager 中增加 VendorConfig。
 */
interface LLMProvider {
    val id: String
    val displayName: String

    /** 分析一餐，返回结构化营养与摘要 */
    suspend fun analyzeMeal(req: MealAnalysisRequest): Result<MealAnalysis>

    /** 基于剩余热量与目标，给出推荐食物 */
    suspend fun suggestFoods(req: FoodSuggestionRequest): Result<List<FoodSuggestion>>
}
