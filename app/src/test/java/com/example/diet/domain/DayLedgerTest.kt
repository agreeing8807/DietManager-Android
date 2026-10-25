package com.example.diet.domain

import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.local.entity.WaterRecord
import com.example.diet.domain.model.DataStatus
import com.example.diet.domain.model.DrinkType
import com.example.diet.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 全应用唯一的统计口径：热量是否含饮品、冲调算多少水、缺失记录怎么处理。
 */
class DayLedgerTest {

    private fun meal(
        calories: Int,
        protein: Float = 0f,
        status: DataStatus = DataStatus.ESTIMATED
    ) = MealRecord(
        date = "2026-10-25",
        type = MealType.LUNCH,
        description = "测试",
        caloriesKcal = calories,
        proteinG = protein,
        carbG = 0f,
        fatG = 0f,
        summary = "",
        status = status
    )

    private fun drink(
        volumeMl: Int,
        waterMl: Int = 0,
        calories: Int = 0,
        protein: Float = 0f
    ) = WaterRecord(
        date = "2026-10-25",
        type = DrinkType.WATER,
        volumeMl = volumeMl,
        waterMl = waterMl,
        drinkName = "水",
        caloriesKcal = calories,
        proteinG = protein
    )

    @Test
    fun waterOfPrefersActualWaterContent() {
        // 牛奶 200ml 只有 174ml 计入水分
        assertEquals(174, DayLedger.waterOf(drink(volumeMl = 200, waterMl = 174)))
    }

    @Test
    fun waterOfFallsBackToVolumeForLegacyRecords() {
        assertEquals(250, DayLedger.waterOf(drink(volumeMl = 250, waterMl = 0)))
    }

    @Test
    fun ofSumsMealsAndDrinksForCalories() {
        val intake = DayLedger.of(
            meals = listOf(meal(500, protein = 30f), meal(300, protein = 10f)),
            drinks = listOf(drink(300, waterMl = 300), drink(330, waterMl = 300, calories = 120, protein = 24f))
        )
        assertEquals(920, intake.caloriesKcal)
        assertEquals(64f, intake.proteinG, 0.001f)
        // 饮水只算水：300 + 300，冲调干粉体积不计入
        assertEquals(600, intake.waterMl)
    }

    @Test
    fun ofCountsIncompleteMeals() {
        val intake = DayLedger.of(
            meals = listOf(
                meal(400),
                meal(0, status = DataStatus.MISSING),
                meal(200, status = DataStatus.PARTIAL),
                meal(300, status = DataStatus.CONFIRMED)
            ),
            drinks = emptyList()
        )
        assertEquals(2, intake.pendingMeals)
        assertEquals(900, intake.caloriesKcal)
    }

    @Test
    fun ofEmptyListsIsEmptyIntake() {
        val intake = DayLedger.of(emptyList(), emptyList())
        assertEquals(DailyIntake.EMPTY, intake)
    }
}
