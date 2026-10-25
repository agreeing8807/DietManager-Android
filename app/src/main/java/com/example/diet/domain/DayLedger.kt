package com.example.diet.domain

import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.local.entity.WaterRecord

/**
 * 单日摄入汇总。所有页面（餐食 / 饮水 / 首页）都从这里取数，
 * 「热量是否包含饮品」「冲调算多少水」这类规则只在本文件里定义一次。
 */
data class DailyIntake(
    val waterMl: Int = 0,
    val caloriesKcal: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    /** 数据不完整（缺失或部分缺失）的餐食条数，供界面提示用 */
    val pendingMeals: Int = 0
) {
    companion object {
        val EMPTY = DailyIntake()
    }
}

object DayLedger {

    /** 计入饮水的量：冲调型按实际加水量，旧数据 waterMl=0 时按容量兜底 */
    fun waterOf(record: WaterRecord): Int =
        if (record.waterMl > 0) record.waterMl else record.volumeMl

    /**
     * 统一口径：
     * - 热量 = 餐食 + 饮品（含冲调干粉带来的热量）；
     * - 饮水量只算水：冲调只计加水量，干粉体积不算饮水；
     * - 缺失数据的记录数值本就为 0，不参与求和，但计入 [DailyIntake.pendingMeals]；
     * - 部分缺失的记录已经按可解析的条目求和，同样计入 pendingMeals。
     */
    fun of(meals: List<MealRecord>, drinks: List<WaterRecord>): DailyIntake = DailyIntake(
        waterMl = drinks.sumOf { waterOf(it) },
        caloriesKcal = meals.sumOf { it.caloriesKcal } + drinks.sumOf { it.caloriesKcal },
        proteinG = meals.fold(0f) { acc, m -> acc + m.proteinG } +
            drinks.fold(0f) { acc, d -> acc + d.proteinG },
        carbG = meals.fold(0f) { acc, m -> acc + m.carbG } +
            drinks.fold(0f) { acc, d -> acc + d.carbG },
        fatG = meals.fold(0f) { acc, m -> acc + m.fatG } +
            drinks.fold(0f) { acc, d -> acc + d.fatG },
        pendingMeals = meals.count { it.status.needsAttention }
    )
}
