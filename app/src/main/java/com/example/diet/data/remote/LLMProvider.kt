package com.example.diet.data.remote

import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.domain.model.FoodSuggestionRequest
import com.example.diet.domain.model.MealAnalysis
import com.example.diet.domain.model.MealAnalysisItem
import com.example.diet.domain.model.MealAnalysisRequest
import com.example.diet.domain.model.NutritionLabel

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

    /**
     * 读取包装上的营养成分表。
     * 只做 OCR + 结构化：返回商品名、标签基准克数与该基准下的四项数值，
     * 每 100g 归一化与按克算热量由本地完成。
     */
    suspend fun readNutritionLabel(imageBytes: ByteArray): Result<NutritionLabel>

    /**
     * 只看份量：估计照片里指定食物的可食用克数，用于给营养成分表补克重。
     * 无法判断时返回 0。
     */
    suspend fun estimateGrams(imageBytes: ByteArray, foodName: String): Result<Float>
}
