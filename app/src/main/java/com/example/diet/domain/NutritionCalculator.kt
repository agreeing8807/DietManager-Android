package com.example.diet.domain

import com.example.diet.domain.model.ActivityLevel
import com.example.diet.domain.model.DrinkType
import com.example.diet.domain.model.Gender
import com.example.diet.domain.model.MacroTargets

/**
 * 营养与目标热量计算（Mifflin-St Jeor 公式）
 */
object NutritionCalculator {

    /** 基础代谢率 BMR (kcal) */
    fun bmr(
        gender: Gender,
        weightKg: Float,
        heightCm: Float,
        age: Int
    ): Float {
        val base = 10f * weightKg + 6.25f * heightCm - 5f * age
        return if (gender == Gender.MALE) base + 5f else base - 161f
    }

    /** 每日总消耗 TDEE (kcal) */
    fun tdee(
        gender: Gender,
        weightKg: Float,
        heightCm: Float,
        age: Int,
        activity: ActivityLevel
    ): Float = bmr(gender, weightKg, heightCm, age) * activity.multiplier

    /**
     * 根据目标体重推算每日热量目标。
     * 减重用 TDEE - 赤字；增重用 TDEE + 盈余；维持用 TDEE。
     */
    fun dailyCalorieGoal(
        gender: Gender,
        weightKg: Float,
        heightCm: Float,
        age: Int,
        activity: ActivityLevel,
        targetWeightKg: Float
    ): Int {
        val tdee = tdee(gender, weightKg, heightCm, age, activity)
        val diff = targetWeightKg - weightKg
        val adjust = when {
            diff < -0.5f -> -500f   // 减重约 0.5kg/周
            diff > 0.5f -> 300f     // 增重
            else -> 0f              // 维持
        }
        return (tdee + adjust).toInt().coerceAtLeast(1200)
    }

    /**
     * 按当前体重与热量目标推算每日蛋白质/碳水/脂肪目标：
     * - 蛋白质 1.6 g/kg（常见增肌/减脂区间），最多不超过总热量 35%
     * - 脂肪占总热量 25%
     * - 碳水用剩余热量补足（1g 蛋白/碳水 = 4 kcal，1g 脂肪 = 9 kcal）
     */
    fun macroTargets(
        calorieGoal: Int,
        weightKg: Float,
        proteinPerKg: Float = 1.6f
    ): MacroTargets {
        val goal = calorieGoal.coerceAtLeast(1200)
        val protein = (proteinPerKg * weightKg).coerceIn(0f, goal * 0.35f / 4f)
        val fat = goal * 0.25f / 9f
        val carbKcal = (goal - protein * 4f - fat * 9f).coerceAtLeast(0f)
        val carb = carbKcal / 4f
        return MacroTargets(
            proteinG = protein,
            carbG = carb,
            fatG = fat,
            proteinKcal = (protein * 4f).toInt(),
            carbKcal = (carb * 4f).toInt(),
            fatKcal = (fat * 9f).toInt(),
            calorieGoal = goal
        )
    }

    /**
     * 估算饮品热量：奶茶/含糖饮品按容量×每100ml热量；纯水为0。
     * 同时返回是否计入水分（奶茶既算热量也算水分）。
     */
    fun estimateDrink(
        type: DrinkType,
        volumeMl: Int,
        explicitCalories: Int? = null
    ): Pair<Int, Boolean> {
        val calories = explicitCalories
            ?: (volumeMl * type.defaultCaloriesPer100ml / 100)
        return calories to type.isMilkTea
    }
}
