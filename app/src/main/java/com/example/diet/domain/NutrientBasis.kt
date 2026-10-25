package com.example.diet.domain

import com.example.diet.data.local.FoodRef
import com.example.diet.data.local.entity.DrinkPreset
import com.example.diet.data.local.entity.MealItem
import com.example.diet.domain.model.DrinkKind
import kotlin.math.roundToInt

/** 一份的计量单位：克、毫升、或「一勺/一个/一包」 */
enum class ServingUnit(val label: String) {
    GRAM("克"),
    MILLILITER("毫升"),
    PIECE("份")
}

/**
 * 全应用唯一的营养基准：**一份 = amountPerUnit（克 / 毫升 / 无意义）+ 每份营养**。
 *
 * 「每 100g」「每 100ml」「每勺干粉」都只是它的不同参数，不再是三种独立结构：
 * - 每 100g 米饭  → ServingBasis(GRAM, 100, 116, 2.6, 25.9, 0.3)
 * - 每 100ml 牛奶 → ServingBasis(MILLILITER, 100, 54, 3.0, 4.8, 3.2)
 * - 每勺蛋白粉    → ServingBasis(PIECE, 30, 120, 24, 3, 1.5)
 *
 * 新增形态（浓缩液、半成品、按块计）只需换 [unit] 与 [amountPerUnit]，不用再加一套字段。
 */
data class ServingBasis(
    val unit: ServingUnit,
    /** 一份对应的量：克数或毫升数；PIECE 且未知时为 0 */
    val amountPerUnit: Float,
    /** 每份营养 */
    val kcal: Float,
    val proteinG: Float,
    val carbG: Float,
    val fatG: Float
) {
    /** 总份数未知是否为有效值（amountPerUnit 与营养都为空时视为无效基准） */
    val isUsable: Boolean
        get() = kcal > 0f || proteinG > 0f || carbG > 0f || fatG > 0f

    /** 按「份数」计算，份数可以是小数（如 1.5 勺） */
    fun totalsOf(units: Float): NutrientTotals {
        val u = units.coerceIn(0f, MAX_UNITS)
        return NutrientTotals(
            kcal = (kcal * u).roundToInt(),
            proteinG = proteinG * u,
            carbG = carbG * u,
            fatG = fatG * u
        )
    }

    /** 按克数/毫升计算；单位量未知时返回空 */
    fun totalsForAmount(amount: Float): NutrientTotals =
        if (amountPerUnit <= 0f) NutrientTotals.EMPTY else totalsOf(amount / amountPerUnit)

    /** 克数 → 份数；未知返回 0 */
    fun unitsForAmount(amount: Float): Float =
        if (amountPerUnit <= 0f) 0f else amount / amountPerUnit

    companion object {
        /** 单次换算的份数上限，防止误输入 */
        const val MAX_UNITS = 100f

        /** 每 100g（食物成分表、餐食条目） */
        fun per100g(per100: Per100g, unit: ServingUnit = ServingUnit.GRAM): ServingBasis =
            ServingBasis(unit, 100f, per100.kcal, per100.proteinG, per100.carbG, per100.fatG)

        /** 每份（一勺干粉等） */
        fun perServing(
            amountPerServing: Float,
            kcal: Float,
            proteinG: Float,
            carbG: Float,
            fatG: Float
        ): ServingBasis = ServingBasis(ServingUnit.PIECE, amountPerServing, kcal, proteinG, carbG, fatG)
    }
}

/**
 * 把各种存储形态换算成同一个 [ServingBasis]。
 * 这是「加形态不改计算」的唯一入口。
 */
object NutrientBasis {

    fun of(ref: FoodRef): ServingBasis = ServingBasis.per100g(ref.per100)

    fun of(item: MealItem): ServingBasis = ServingBasis(
        unit = ServingUnit.GRAM,
        amountPerUnit = 100f,
        kcal = item.kcalPer100g,
        proteinG = item.proteinPer100g,
        carbG = item.carbPer100g,
        fatG = item.fatPer100g
    )

    fun of(preset: DrinkPreset): ServingBasis =
        if (preset.kind == DrinkKind.POWDER) {
            ServingBasis.perServing(
                amountPerServing = preset.servingGrams,
                kcal = preset.caloriesPerServing,
                proteinG = preset.proteinPerServing,
                carbG = preset.carbPerServing,
                fatG = preset.fatPerServing
            )
        } else {
            ServingBasis(
                unit = ServingUnit.MILLILITER,
                amountPerUnit = 100f,
                kcal = preset.caloriesPer100ml,
                proteinG = preset.proteinPer100ml,
                carbG = preset.carbPer100ml,
                fatG = preset.fatPer100ml
            )
        }
}
