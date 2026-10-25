package com.example.diet.domain

import com.example.diet.data.local.entity.DrinkPreset
import com.example.diet.domain.model.DrinkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 冲调型的核心承诺：营养只随份数变化，水量只影响总容量与浓度。
 */
class DrinkCalculatorTest {

    private val milk = DrinkPreset(
        name = "牛奶",
        waterRatio = 0.87f,
        caloriesPer100ml = 54f,
        proteinPer100ml = 3.0f,
        carbPer100ml = 4.8f,
        fatPer100ml = 3.2f
    )

    private val proteinPowder = DrinkPreset(
        name = "蛋白粉",
        kind = DrinkKind.POWDER,
        servingGrams = 30f,
        caloriesPerServing = 120f,
        proteinPerServing = 24f,
        carbPerServing = 3f,
        fatPerServing = 1.5f
    )

    @Test
    fun liquidNutritionConvertsByWaterRatio() {
        val result = DrinkCalculator.nutritionOf(milk, 200)
        assertEquals(174, result.waterMl)
        assertEquals(108, result.caloriesKcal)
        assertEquals(6f, result.proteinG, 0.001f)
    }

    @Test
    fun mixCombinesWaterAndPowderVolume() {
        val mix = DrinkCalculator.mixOf(proteinPowder, servings = 1f, waterMl = 300)
        assertEquals(330, mix.totalMl)
        assertEquals(300, mix.waterMl)
        assertEquals(30f, mix.powderGrams, 0.001f)
        assertEquals(120, mix.caloriesKcal)
        assertEquals(24f, mix.proteinG, 0.001f)
    }

    @Test
    fun waterOnlyChangesVolumeAndConcentration() {
        val thin = DrinkCalculator.mixOf(proteinPowder, servings = 1f, waterMl = 500)
        val rich = DrinkCalculator.mixOf(proteinPowder, servings = 1f, waterMl = 100)

        // 营养总量完全一致
        assertEquals(rich.caloriesKcal, thin.caloriesKcal)
        assertEquals(rich.proteinG, thin.proteinG, 0.001f)
        assertEquals(rich.carbG, thin.carbG, 0.001f)
        assertEquals(rich.fatG, thin.fatG, 0.001f)

        // 总容量与浓度不同
        assertTrue(thin.totalMl > rich.totalMl)
        assertTrue(thin.kcalPer100ml < rich.kcalPer100ml)
    }

    @Test
    fun nutritionScalesWithServings() {
        val two = DrinkCalculator.mixOf(proteinPowder, servings = 2f, waterMl = 300)
        assertEquals(240, two.caloriesKcal)
        assertEquals(48f, two.proteinG, 0.001f)
        assertEquals(60f, two.powderGrams, 0.001f)
        // 总容量 = 300 + 60
        assertEquals(360, two.totalMl)
    }

    @Test
    fun mixClampsInvalidInputs() {
        // 0 份：没有干粉体积，总容量只剩水
        val zero = DrinkCalculator.mixOf(proteinPowder, servings = 0f, waterMl = 300)
        assertEquals(0, zero.caloriesKcal)
        assertEquals(0f, zero.proteinG, 0.001f)
        assertEquals(300, zero.totalMl)

        val huge = DrinkCalculator.mixOf(proteinPowder, servings = 999f, waterMl = 99999)
        assertTrue(huge.servings <= 20f)
        assertTrue(huge.waterMl <= 5000)
    }
}
