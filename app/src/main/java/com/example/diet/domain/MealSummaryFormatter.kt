package com.example.diet.domain

import java.util.Locale
import kotlin.math.roundToInt

/**
 * 餐食摘要模板：原本由大模型生成的“份量/每 100g/来源”固定文案改为本地拼装，
 * 结构统一、零 token、且可持久化（MealRecord 只保存 summary）。
 */
object MealSummaryFormatter {

    fun build(
        items: List<ResolvedItem>,
        totals: NutrientTotals,
        gramsSum: Float,
        sources: List<String>,
        missingNames: List<String>,
        conflict: String,
        imageIgnored: Boolean
    ): String {
        val lines = mutableListOf<String>()
        val resolved = items.filter { it.per100 != null }

        if (resolved.isEmpty()) {
            lines += "本次营养数值待补充，可点开明细修正"
        } else {
            val names = buildString {
                append(resolved.take(3).joinToString("、") { "${it.name} ${it.grams.roundToInt()}g" })
                if (resolved.size > 3) append(" 等")
            }
            if (gramsSum > 0f) {
                lines += "识别：$names（共 ${gramsSum.roundToInt()}g）"
            } else {
                lines += "识别：$names"
            }

            NutritionTools.averagePer100(totals, gramsSum)?.let { avg ->
                lines += "综合每 100g：约 ${avg.kcal.roundToInt()} kcal / 蛋白 ${f1(avg.proteinG)} g / " +
                    "碳水 ${f1(avg.carbG)} g / 脂肪 ${f1(avg.fatG)} g"
            }

            if (sources.isNotEmpty()) {
                lines += "来源：${sources.distinct().joinToString("、")}"
            }
        }

        if (missingNames.isNotEmpty()) {
            lines += "「${missingNames.distinct().joinToString("、")}」暂未计入合计，可点开明细补充"
        }
        if (conflict.isNotBlank()) lines += "描述与图片不一致：$conflict"
        if (imageIgnored) lines += "（当前模型不支持图片，已按文字描述估算）"

        return lines.joinToString("\n")
    }

    private fun f1(value: Float): String = String.format(Locale.CHINA, "%.1f", value)
}
