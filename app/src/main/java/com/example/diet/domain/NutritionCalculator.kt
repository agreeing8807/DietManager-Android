package com.example.diet.domain

import com.example.diet.domain.model.ActivityLevel
import com.example.diet.domain.model.DrinkType
import com.example.diet.domain.model.Gender

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
