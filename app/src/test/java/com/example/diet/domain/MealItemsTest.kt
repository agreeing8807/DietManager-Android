package com.example.diet.domain

import com.example.diet.data.local.entity.MealItem
import com.example.diet.domain.model.DataStatus
import com.example.diet.domain.model.MealAnalysisItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 条目是营养数据的真源：明细的换算、求和、状态推导都在这里锁定。
 */
class MealItemsTest {

    private fun item(
        name: String = "米饭",
        grams: Float = 200f,
        kcalPer100g: Float = 116f,
        proteinPer100g: Float = 2.6f,
        status: DataStatus = DataStatus.ESTIMATED
    ) = MealItem(
        recordId = 1L,
        name = name,
        grams = grams,
        kcalPer100g = kcalPer100g,
        proteinPer100g = proteinPer100g,
        carbPer100g = 25.9f,
        fatPer100g = 0.3f,
        status = status
    )

    @Test
    fun nutritionOfUsesPer100AndGrams() {
        val totals = MealItems.nutritionOf(item())
        assertEquals(232, totals.kcal)
        assertEquals(5.2f, totals.proteinG, 0.001f)
    }

    @Test
    fun nutritionOfCanUseOverriddenGrams() {
        assertEquals(116, MealItems.nutritionOf(item(), 100f).kcal)
    }

    @Test
    fun nutritionOfIsEmptyWhenNoBasis() {
        val noBasis = MealItem(recordId = 1L, name = "某食堂菜", grams = 300f)
        val totals = MealItems.nutritionOf(noBasis)
        assertEquals(0, totals.kcal)
        assertFalse(noBasis.hasBasis)
    }

    @Test
    fun totalsSumsAllItems() {
        val totals = MealItems.totals(listOf(item(), item(name = "鸡胸肉", grams = 100f)))
        assertEquals(232 + 116, totals.kcal)
    }

    @Test
    fun statusIsMissingWhenNoItems() {
        assertEquals(DataStatus.MISSING, MealItems.status(emptyList()))
    }

    @Test
    fun statusIsMissingWhenNoItemHasBasis() {
        val items = listOf(MealItem(recordId = 1L, name = "套餐", grams = 300f))
        assertEquals(DataStatus.MISSING, MealItems.status(items))
    }

    @Test
    fun statusIsPartialWhenSomeItemsLackBasis() {
        val items = listOf(item(), MealItem(recordId = 1L, name = "盖饭", grams = 300f))
        assertEquals(DataStatus.PARTIAL, MealItems.status(items))
    }

    @Test
    fun statusIsEstimatedByDefault() {
        assertEquals(DataStatus.ESTIMATED, MealItems.status(listOf(item())))
    }

    @Test
    fun statusIsConfirmedOnlyWhenAllItemsConfirmed() {
        val confirmed = item(status = DataStatus.CONFIRMED)
        assertEquals(DataStatus.CONFIRMED, MealItems.status(listOf(confirmed)))

        val mixed = listOf(confirmed, item(name = "鸡蛋"))
        assertEquals(DataStatus.ESTIMATED, MealItems.status(mixed))
    }

    @Test
    fun entityRoundTripKeepsBasis() {
        val analysis = MealAnalysisItem(
            name = "鸡胸肉",
            grams = 150f,
            brand = "",
            per100 = Per100g(133f, 24.6f, 0.6f, 3.6f),
            source = "内置食物成分表",
            confidence = 1f
        )
        val entity = MealItems.toEntity(recordId = 9L, index = 2, item = analysis)
        assertEquals(9L, entity.recordId)
        assertEquals(2, entity.sortOrder)
        assertTrue(entity.hasBasis)

        val back = MealItems.toAnalysisItem(entity)
        assertEquals(analysis.name, back.name)
        assertEquals(analysis.grams, back.grams, 0.001f)
        assertEquals(analysis.per100, back.per100)
    }

    @Test
    fun toAnalysisItemKeepsNullBasisForMissingItem() {
        val entity = MealItem(recordId = 1L, name = "未知", grams = 200f)
        assertNull(MealItems.toAnalysisItem(entity).per100)
    }
}
