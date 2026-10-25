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
 * 按每 100ml 的比例换算饮品营养，不调用任何 API。
 * 例：牛奶 200ml → 水分 174ml、热量 108kcal、蛋白质 6g。
 */
object DrinkCalculator {

    fun nutritionOf(preset: DrinkPreset, volumeMl: Int): DrinkNutrition {
        val factor = volumeMl / 100f
        return DrinkNutrition(
            waterMl = (volumeMl * preset.waterRatio.coerceIn(0f, 1f)).roundToInt(),
            caloriesKcal = (preset.caloriesPer100ml * factor).roundToInt(),
            proteinG = preset.proteinPer100ml * factor,
            carbG = preset.carbPer100ml * factor,
            fatG = preset.fatPer100ml * factor
        )
    }
}
