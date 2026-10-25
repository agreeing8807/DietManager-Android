package com.example.diet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 营养换算与校验是「本地计算优先」的底座，全部为纯函数，这里锁定行为。
 */
class NutritionToolsTest {

    private val rice = Per100g(kcal = 116f, proteinG = 2.6f, carbG = 25.9f, fatG = 0.3f)

    @Test
    fun computeConvertsByGrams() {
        val totals = NutritionTools.compute(rice, 200f)
        assertEquals(232, totals.kcal)
        assertEquals(5.2f, totals.proteinG, 0.001f)
        assertEquals(51.8f, totals.carbG, 0.001f)
        assertEquals(0.6f, totals.fatG, 0.001f)
    }

    @Test
    fun computeClampsOutOfRangeGrams() {
        assertEquals(0, NutritionTools.compute(rice, -50f).kcal)
        // 上限 5000g：116 * 50 = 5800
        assertEquals(5800, NutritionTools.compute(rice, 99999f).kcal)
    }

    @Test
    fun sumSkipsItemsWithoutNutritionData() {
        val totals = NutritionTools.sum(
            listOf(
                ResolvedItem("米饭", 100f, rice),
                ResolvedItem("未知食物", 300f, null),
                ResolvedItem("米饭", 100f, rice)
            )
        )
        assertEquals(232, totals.kcal)
        assertEquals(5.2f, totals.proteinG, 0.001f)
    }

    @Test
    fun validateZeroesNegativeValues() {
        val result = NutritionTools.validate(NutrientTotals(-10, -1f, 0f, 0f))
        assertEquals(0, result.totals.kcal)
        assertEquals(0f, result.totals.proteinG, 0.001f)
        assertFalse(result.ok)
    }

    @Test
    fun validateRecomputesKcalWhenMacroMismatch() {
        // 三大营养素 = 10*4 + 10*4 + 10*9 = 170 kcal，与声明的 500 kcal 差距过大
        val result = NutritionTools.validate(NutrientTotals(500, 10f, 10f, 10f))
        assertEquals(170, result.totals.kcal)
        assertTrue(result.violations.any { it.contains("4/4/9") })
    }

    @Test
    fun validateKeepsConsistentValuesUntouched() {
        val result = NutritionTools.validate(NutrientTotals(310, 25f, 30f, 10f))
        assertEquals(310, result.totals.kcal)
        assertTrue(result.ok)
    }

    @Test
    fun validateCapsSingleMealAt3000() {
        val result = NutritionTools.validate(NutrientTotals(3200, 320f, 320f, 100f))
        assertEquals(3000, result.totals.kcal)
        assertTrue(result.violations.any { it.contains("封顶") })
    }

    @Test
    fun averagePer100ReturnsNullWhenGramsSumIsZero() {
        assertNull(NutritionTools.averagePer100(NutrientTotals.EMPTY, 0f))
    }

    @Test
    fun averagePer100NormalizesToHundredGrams() {
        val average = NutritionTools.averagePer100(NutrientTotals(232, 5.2f, 51.8f, 0.6f), 200f)
        assertEquals(116f, average!!.kcal, 0.01f)
        assertEquals(2.6f, average.proteinG, 0.01f)
    }
}
