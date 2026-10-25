package com.example.diet.domain

import com.example.diet.data.local.entity.MealItem
import com.example.diet.domain.model.DataStatus
import com.example.diet.domain.model.MealAnalysisItem

/**
 * 条目级换算：MealItem 只存每 100g 基准与克数，绝对值在这里算。
 * 用户改克数后无需再调用模型，记录头由 [totals] / [status] 重算。
 */
object MealItems {

    /** 明细 → 分析条目（用于把缺失项交还给模型补数） */
    fun toAnalysisItem(item: MealItem): MealAnalysisItem = MealAnalysisItem(
        name = item.name,
        grams = item.grams,
        servings = item.servings,
        brand = item.brand,
        per100 = if (item.hasBasis) {
            Per100g(item.kcalPer100g, item.proteinPer100g, item.carbPer100g, item.fatPer100g)
        } else {
            null
        },
        source = item.source,
        confidence = item.confidence,
        status = item.status
    )

    /** 分析条目 → 明细（id 由数据库生成或沿用旧值） */
    fun toEntity(recordId: Long, index: Int, item: MealAnalysisItem): MealItem = MealItem(
        recordId = recordId,
        name = item.name,
        brand = item.brand,
        grams = item.grams,
        servings = item.servings,
        kcalPer100g = item.per100?.kcal ?: 0f,
        proteinPer100g = item.per100?.proteinG ?: 0f,
        carbPer100g = item.per100?.carbG ?: 0f,
        fatPer100g = item.per100?.fatG ?: 0f,
        source = item.source,
        confidence = item.confidence,
        status = item.status,
        sortOrder = index
    )

    fun nutritionOf(item: MealItem, grams: Float = item.grams): NutrientTotals {
        val basis = NutrientBasis.of(item)
        if (!basis.isUsable) return NutrientTotals.EMPTY
        return basis.totalsForAmount(grams.coerceIn(0f, NutritionTools.MAX_GRAMS))
    }

    fun totals(items: List<MealItem>): NutrientTotals = items.fold(NutrientTotals.EMPTY) { acc, item ->
        val one = nutritionOf(item)
        NutrientTotals(
            kcal = acc.kcal + one.kcal,
            proteinG = acc.proteinG + one.proteinG,
            carbG = acc.carbG + one.carbG,
            fatG = acc.fatG + one.fatG
        )
    }

    /**
     * 整餐状态由条目推导：
     * 无条目、或所有条目都没有基准 → 缺失；部分条目缺基准 → 部分缺失；
     * 全部由用户确认过 → 已确认；其余 → 估算。
     */
    fun status(items: List<MealItem>): DataStatus = when {
        items.isEmpty() -> DataStatus.MISSING
        items.none { it.hasBasis } -> DataStatus.MISSING
        items.any { !it.hasBasis } -> DataStatus.PARTIAL
        items.all { it.status == DataStatus.CONFIRMED } -> DataStatus.CONFIRMED
        else -> DataStatus.ESTIMATED
    }
}
