package com.example.diet.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** 每 100g（饮品按每 100ml）营养 */
data class Per100g(
    val kcal: Float,
    val proteinG: Float,
    val carbG: Float,
    val fatG: Float
)

/** 一份/一餐的营养合计 */
data class NutrientTotals(
    val kcal: Int,
    val proteinG: Float,
    val carbG: Float,
    val fatG: Float
) {
    companion object {
        val EMPTY = NutrientTotals(0, 0f, 0f, 0f)
    }
}

/** 识别出的单项食物 + 解析到的营养（per100 为空表示未找到数据） */
data class ResolvedItem(
    val name: String,
    val grams: Float,
    val per100: Per100g?,
    val source: String = "",
    /** 数据可靠程度：本地成分表为 1，模型补数为模型自评，未知为 0 */
    val confidence: Float = 0f
)

/** 校验结果：totals 可能被修正，violations 记录修正原因 */
data class ValidationResult(
    val totals: NutrientTotals,
    val violations: List<String>
) {
    val ok: Boolean get() = violations.isEmpty()
}

/**
 * 营养计算与校验工具：原本由大模型完成的换算、求和、合理性判断全部本地化，
 * 纯函数、无 IO、零 token。
 */
object NutritionTools {

    /** 单次换算的克数上限 */
    const val MAX_GRAMS = 5000f

    /** 按每 100g 数据换算到实际克数；克数越界按 0~5000g 处理 */
    fun compute(per100: Per100g, grams: Float): NutrientTotals =
        ServingBasis.per100g(per100).totalsForAmount(grams.coerceIn(0f, MAX_GRAMS))

    /** 多项求和，未解析到数据的项自动跳过 */
    fun sum(items: List<ResolvedItem>): NutrientTotals =
        items.fold(NutrientTotals.EMPTY) { acc, item ->
            val per100 = item.per100 ?: return@fold acc
            val t = compute(per100, item.grams)
            NutrientTotals(
                kcal = acc.kcal + t.kcal,
                proteinG = acc.proteinG + t.proteinG,
                carbG = acc.carbG + t.carbG,
                fatG = acc.fatG + t.fatG
            )
        }

    /**
     * 合理性校验：
     * 1. 负值归零；
     * 2. 能量与 4/4/9 计算值偏差过大时以宏量重算（±30% 或 80 kcal 容差）；
     * 3. 单餐热量封顶 3000 kcal。
     */
    fun validate(totals: NutrientTotals): ValidationResult {
        val violations = mutableListOf<String>()
        var t = totals

        if (t.kcal < 0 || t.proteinG < 0f || t.carbG < 0f || t.fatG < 0f) {
            violations += "检测到负值，已归零"
            t = NutrientTotals(
                kcal = t.kcal.coerceAtLeast(0),
                proteinG = t.proteinG.coerceAtLeast(0f),
                carbG = t.carbG.coerceAtLeast(0f),
                fatG = t.fatG.coerceAtLeast(0f)
            )
        }

        val macroKcal = t.proteinG * 4f + t.carbG * 4f + t.fatG * 9f
        if (t.kcal > 0 && abs(macroKcal - t.kcal) > max(80f, t.kcal * 0.3f)) {
            violations += "能量与三大营养素不一致，已按 4/4/9 重算"
            t = t.copy(kcal = macroKcal.roundToInt())
        }

        if (t.kcal > 3000) {
            violations += "单餐热量超过 3000 kcal，已封顶"
            t = t.copy(kcal = 3000)
        }

        return ValidationResult(totals = t, violations = violations)
    }

    /** 综合每 100g（用于摘要展示），gramsSum <= 0 时返回 null */
    fun averagePer100(totals: NutrientTotals, gramsSum: Float): Per100g? {
        if (gramsSum <= 0f) return null
        val f = 100f / gramsSum
        return Per100g(
            kcal = totals.kcal * f,
            proteinG = totals.proteinG * f,
            carbG = totals.carbG * f,
            fatG = totals.fatG * f
        )
    }
}
