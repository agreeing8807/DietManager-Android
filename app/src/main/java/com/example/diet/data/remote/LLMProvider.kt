package com.example.diet.data.remote

import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.domain.model.FoodSuggestionRequest
import com.example.diet.domain.model.MealAnalysis
import com.example.diet.domain.model.MealAnalysisItem
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

    /**
     * 只为缺少营养数据的条目补每 100g 基准，不重新识别、不改克数。
     * 返回顺序与入参一致；仍无法补全的条目原样返回。
     */
    suspend fun fillNutrition(items: List<MealAnalysisItem>): Result<List<MealAnalysisItem>>

    /** 基于剩余热量与目标，给出推荐食物 */
    suspend fun suggestFoods(req: FoodSuggestionRequest): Result<List<FoodSuggestion>>
}
