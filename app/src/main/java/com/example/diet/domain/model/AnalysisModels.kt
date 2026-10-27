package com.example.diet.domain.model

import com.example.diet.domain.Per100g

/** 分析出的单项食物：只保留每 100g 基准与克数，绝对值由本地换算 */
data class MealAnalysisItem(
    val name: String,
    val grams: Float,
    /** 可数单位的份数（蛋/包子/勺），0 表示按克记录 */
    val servings: Float = 0f,
    val brand: String = "",
    /** 为空表示没查到可靠营养数据，仍保留条目以便用户补充或删除 */
    val per100: Per100g? = null,
    val source: String = "",
    val confidence: Float = 0f,
    val status: DataStatus = DataStatus.ESTIMATED
)

/** 单餐分析结果（识别结果 + 本地/模型补数计算得出） */
data class MealAnalysis(
    val caloriesKcal: Int,
    val proteinG: Float,
    val carbG: Float,
    val fatG: Float,
    val summary: String,
    /** 整餐的简短菜名，用于拍照添加时的标题 */
    val title: String = "",
    /** 本次数据的可信度，落库时直接沿用，界面不再解析文案 */
    val status: DataStatus = DataStatus.ESTIMATED,
    /** 条目明细，落库到 meal_item，供后续修正与统计 */
    val items: List<MealAnalysisItem> = emptyList()
)

/**
 * 包装食品的营养成分表读数。
 * 模型只读出「标签基准 + 该基准下的数值」，换算到每 100g 与按克算热量全部在本地完成，
 * 避免让模型做除法导致误差累积。
 */
data class NutritionLabel(
    val name: String,
    val brand: String = "",
    /** 标签基准克数：每 100g 为 100，每份 30g 为 30 */
    val basisGrams: Float = 100f,
    /** 标签基准下的原始数值 */
    val kcal: Float = 0f,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    /** 液体饮料按每 100ml 标注 */
    val liquid: Boolean = false,
    val source: String = "包装营养标签"
) {
    private val factor: Float get() = if (basisGrams > 0f) 100f / basisGrams else 0f

    val kcalPer100: Float get() = kcal * factor
    val proteinPer100: Float get() = proteinG * factor
    val carbPer100: Float get() = carbG * factor
    val fatPer100: Float get() = fatG * factor

    /** 每 100g 还是每 100ml，用于界面文案 */
    val baseUnit: String get() = if (liquid) "ml" else "g"

    /** 是否读到了可用数值 */
    val isUsable: Boolean
        get() = basisGrams > 0f &&
            (kcal > 0f || proteinG > 0f || carbG > 0f || fatG > 0f)

    /** 商品显示名：品牌 + 名称 */
    val displayName: String
        get() = if (brand.isBlank()) name else "$brand $name"
}

/** 每日三大营养素目标（克）与折算热量（kcal） */
data class MacroTargets(
    val proteinG: Float,
    val carbG: Float,
    val fatG: Float,
    val proteinKcal: Int,
    val carbKcal: Int,
    val fatKcal: Int,
    val calorieGoal: Int
) {
    /** 三大营养素热量占比（0f~1f） */
    val proteinRatio: Float get() = if (calorieGoal > 0) proteinKcal.toFloat() / calorieGoal else 0f
    val carbRatio: Float get() = if (calorieGoal > 0) carbKcal.toFloat() / calorieGoal else 0f
    val fatRatio: Float get() = if (calorieGoal > 0) fatKcal.toFloat() / calorieGoal else 0f
}

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
    val date: String,
    /** 当前系统时间，如「2026-10-25 18:30 周日」，用于按时段推荐 */
    val currentTime: String = "",
    /** 当前用餐时段，如「晚餐」，空串表示未知 */
    val mealPeriod: String = ""
)
