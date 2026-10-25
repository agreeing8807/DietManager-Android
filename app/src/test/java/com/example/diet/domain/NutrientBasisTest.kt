package com.example.diet.domain

import com.example.diet.data.local.FoodCompositionTable
import com.example.diet.data.local.entity.DrinkPreset
import com.example.diet.data.local.entity.MealItem
import com.example.diet.domain.model.DrinkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 统一营养基准：「每 100g」「每 100ml」「每份」都只是 ServingBasis 的不同参数。
 */
class NutrientBasisTest {

    @Test
    fun per100gBasisConvertsByAmount() {
        val basis = ServingBasis.per100g(Per100g(116f, 2.6f, 25.9f, 0.3f))
        assertEquals(ServingUnit.GRAM, basis.unit)
        assertEquals(100f, basis.amountPerUnit, 0.001f)

        val totals = basis.totalsForAmount(200f)
        assertEquals(232, totals.kcal)
        assertEquals(5.2f, totals.proteinG, 0.001f)
        // 200g = 2 份
        assertEquals(2f, basis.unitsForAmount(200f), 0.001f)
    }

    @Test
    fun perServingBasisMultipliesByServings() {
        val basis = ServingBasis.perServing(30f, 120f, 24f, 3f, 1.5f)
        assertEquals(ServingUnit.PIECE, basis.unit)

        val oneAndHalf = basis.totalsOf(1.5f)
        assertEquals(180, oneAndHalf.kcal)
        assertEquals(36f, oneAndHalf.proteinG, 0.001f)
        assertEquals(4.5f, oneAndHalf.carbG, 0.001f)
        assertEquals(2.25f, oneAndHalf.fatG, 0.001f)
    }

    @Test
    fun amountConversionIsEmptyWhenUnitAmountUnknown() {
        val basis = ServingBasis(ServingUnit.PIECE, 0f, 100f, 10f, 0f, 0f)
        assertEquals(0f, basis.unitsForAmount(200f), 0.001f)
        assertEquals(NutrientTotals.EMPTY, basis.totalsForAmount(200f))
        // 但按份数仍然可用
        assertEquals(100, basis.totalsOf(1f).kcal)
    }

    @Test
    fun unitsAreClampedAndNegativeAmountsBecomeZero() {
        val basis = ServingBasis.perServing(30f, 120f, 24f, 3f, 1.5f)
        assertEquals(120 * ServingBasis.MAX_UNITS.toInt(), basis.totalsOf(9999f).kcal)
        assertEquals(0, basis.totalsOf(-1f).kcal)
        assertEquals(0, basis.totalsForAmount(-100f).kcal)
    }

    @Test
    fun liquidPresetMapsToPer100Milliliter() {
        val milk = DrinkPreset(
            name = "牛奶",
            waterRatio = 0.87f,
            caloriesPer100ml = 54f,
            proteinPer100ml = 3f
        )
        val basis = NutrientBasis.of(milk)
        assertEquals(ServingUnit.MILLILITER, basis.unit)
        assertEquals(108, basis.totalsForAmount(200f).kcal)
    }

    @Test
    fun powderPresetMapsToPerServing() {
        val powder = DrinkPreset(
            name = "蛋白粉",
            kind = DrinkKind.POWDER,
            servingGrams = 30f,
            caloriesPerServing = 120f,
            proteinPerServing = 24f
        )
        val basis = NutrientBasis.of(powder)
        assertEquals(ServingUnit.PIECE, basis.unit)
        assertEquals(30f, basis.amountPerUnit, 0.001f)
        assertEquals(240, basis.totalsOf(2f).kcal)
    }

    @Test
    fun mealItemAndFoodRefMapToSameBasis() {
        val item = MealItem(
            recordId = 1L,
            name = "鸡蛋",
            grams = 50f,
            kcalPer100g = 143f,
            proteinPer100g = 12.6f,
            carbPer100g = 1.1f,
            fatPer100g = 9.5f
        )
        val egg = FoodCompositionTable.lookup("鸡蛋")!!

        assertEquals(NutrientBasis.of(egg).totalsForAmount(50f), NutrientBasis.of(item).totalsForAmount(50f))
        assertEquals(72, NutrientBasis.of(item).totalsForAmount(50f).kcal)
    }

    @Test
    fun foodRefProvidesServingGramsForCountableFoods() {
        val egg = FoodCompositionTable.lookup("鸡蛋")!!
        assertEquals(50f, egg.servingGrams, 0.001f)
        // 两个鸡蛋 = 100g → 143 kcal
        assertEquals(143, NutrientBasis.of(egg).totalsForAmount(egg.servingGrams * 2).kcal)

        val rice = FoodCompositionTable.lookup("米饭")!!
        assertEquals(200f, rice.servingGrams, 0.001f)

        // 不是按份计的食物保持 0，避免瞎推导
        val broccoli = FoodCompositionTable.lookup("西兰花")!!
        assertEquals(0f, broccoli.servingGrams, 0.001f)
    }

    @Test
    fun basisUsabilityFollowsNutritionValues() {
        assertTrue(ServingBasis.per100g(Per100g(1f, 0f, 0f, 0f)).isUsable)
        assertFalse(ServingBasis.per100g(Per100g(0f, 0f, 0f, 0f)).isUsable)
    }
}
