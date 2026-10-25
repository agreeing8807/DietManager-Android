package com.example.diet.domain

import com.example.diet.data.local.entity.DrinkPreset
import kotlin.math.roundToInt

/** 一次饮用按配方换算出的水分与营养 */
data class DrinkNutrition(
    val waterMl: Int,
    val caloriesKcal: Int,
    val proteinG: Float,
    val carbG: Float,
    val fatG: Float
)

/**
 * 一次冲调的结果。
 * 营养总额只由 [servings] 决定；[waterMl] 仅影响 [totalMl] 与 [kcalPer100ml]（浓度）。
 */
data class PowderNutrition(
    val servings: Float,
    /** 实际加水量，全部计入当日饮水 */
    val waterMl: Int,
    /** 干粉总重 = 每份克数 × 份数 */
    val powderGrams: Float,
    /** 冲调后的总容量（加水量 + 干粉体积） */
    val totalMl: Int,
    val caloriesKcal: Int,
    val proteinG: Float,
    val carbG: Float,
    val fatG: Float,
    /** 浓度：每 100ml 的热量 */
    val kcalPer100ml: Float
)

/**
 * 饮品营养换算，不调用任何 API。
 * 例：牛奶 200ml → 水分 174ml、热量 108kcal、蛋白质 6g；
 *     蛋白粉 1 勺（30g，蛋白 24g）冲 300ml 水 → 蛋白 24g、总容量 330ml。
 */
object DrinkCalculator {

    /** 干粉体积近似 1g ≈ 1ml，仅用于估算总容量与浓度 */
    private const val POWDER_ML_PER_GRAM = 1f

    fun nutritionOf(preset: DrinkPreset, volumeMl: Int): DrinkNutrition {
        val totals = NutrientBasis.of(preset).totalsForAmount(volumeMl.toFloat())
        return DrinkNutrition(
            waterMl = (volumeMl * preset.waterRatio.coerceIn(0f, 1f)).roundToInt(),
            caloriesKcal = totals.kcal,
            proteinG = totals.proteinG,
            carbG = totals.carbG,
            fatG = totals.fatG
        )
    }

    /**
     * 冲调型换算：营养 = 每份干粉营养 × 份数，与加水量无关；
     * 加水量只决定总容量与浓度，因此同份数下多加水的只是「更淡」。
     */
    fun mixOf(preset: DrinkPreset, servings: Float, waterMl: Int): PowderNutrition {
        val s = servings.coerceIn(0f, MAX_SERVINGS)
        val water = waterMl.coerceIn(0, MAX_WATER_ML)
        // 营养只取「每份」基准 × 份数，与水完全无关
        val totals = NutrientBasis.of(preset).totalsOf(s)
        val powderGrams = preset.servingGrams.coerceAtLeast(0f) * s
        val totalMl = (water + powderGrams * POWDER_ML_PER_GRAM).roundToInt()
        return PowderNutrition(
            servings = s,
            waterMl = water,
            powderGrams = powderGrams,
            totalMl = totalMl,
            caloriesKcal = totals.kcal,
            proteinG = totals.proteinG,
            carbG = totals.carbG,
            fatG = totals.fatG,
            kcalPer100ml = if (totalMl > 0) totals.kcal * 100f / totalMl else 0f
        )
    }

    /** 单次冲调份数上限，防止误输入导致离谱数据 */
    private const val MAX_SERVINGS = 20f
    private const val MAX_WATER_ML = 5000
}
