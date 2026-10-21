package com.example.diet.domain.model

/** 单餐分析结果（来自大模型，结构化解析） */
data class MealAnalysis(
    val caloriesKcal: Int,
    val proteinG: Float,
    val carbG: Float,
    val fatG: Float,
    val summary: String
)

/** 推荐食物条目 */
data class FoodSuggestion(
    val name: String,
    val caloriesKcal: Int,
    val reason: String
)

/** 三餐分析请求（文字为主，可选图片） */
data class MealAnalysisRequest(
    val description: String,
    val imageBytes: ByteArray? = null,
    val userContext: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MealAnalysisRequest
        return description == other.description &&
            imageBytes.contentEquals(other.imageBytes) &&
            userContext == other.userContext
    }

    override fun hashCode(): Int {
        var result = description.hashCode()
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + userContext.hashCode()
        return result
    }
}

/** 食物推荐请求 */
data class FoodSuggestionRequest(
    val remainingCalories: Int,
    val context: String,
    val date: String
)
