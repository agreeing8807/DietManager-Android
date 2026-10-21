package com.example.diet.domain.model

import kotlinx.serialization.Serializable

/** 性别 */
@Serializable
enum class Gender {
    MALE, FEMALE
}

/** 身体活动水平，用于推算 TDEE */
@Serializable
enum class ActivityLevel(
    val label: String,
    val multiplier: Float
) {
    SEDENTARY("久坐（几乎不运动）", 1.2f),
    LIGHT("轻度（每周 1-3 次）", 1.375f),
    MODERATE("中度（每周 3-5 次）", 1.55f),
    ACTIVE("高度（每周 6-7 次）", 1.725f),
    VERY_ACTIVE("极高（体力工作/每日训练）", 1.9f)
}

/** 餐次 */
@Serializable
enum class MealType(val label: String) {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐"),
    SNACK("加餐")
}

/** 饮品类型 */
@Serializable
enum class DrinkType(
    val label: String,
    val defaultCaloriesPer100ml: Int,
    val isMilkTea: Boolean
) {
    WATER("水", 0, false),
    TEA("茶", 0, false),
    COFFEE("咖啡", 5, false),
    MILK_TEA("奶茶", 65, true),
    OTHER("其他", 30, false)
}
