package com.example.diet.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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

/** 餐次（含三个加餐时段） */
@Serializable(with = MealTypeSerializer::class)
enum class MealType(val label: String) {
    BREAKFAST("早餐"),
    MORNING_SNACK("早加餐"),
    LUNCH("午餐"),
    AFTERNOON_SNACK("午加餐"),
    DINNER("晚餐"),
    EVENING_SNACK("晚加餐");

    companion object {
        /** 按系统时间自动判断餐次 */
        fun fromHour(hour: Int): MealType = when (hour) {
            in 5..8 -> BREAKFAST
            in 9..10 -> MORNING_SNACK
            in 11..13 -> LUNCH
            in 14..15 -> AFTERNOON_SNACK
            in 16..19 -> DINNER
            else -> EVENING_SNACK
        }

        /** 兼容历史数据中的旧枚举名 */
        fun fromNameOrDefault(raw: String?): MealType = when (raw) {
            null -> EVENING_SNACK
            "SNACK" -> EVENING_SNACK
            else -> values().firstOrNull { it.name == raw } ?: BREAKFAST
        }
    }
}

/** 旧备份里的 "SNACK" 也能正常反序列化 */
object MealTypeSerializer : KSerializer<MealType> {
    override val descriptor = PrimitiveSerialDescriptor("MealType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MealType) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): MealType = MealType.fromNameOrDefault(decoder.decodeString())
}

/** 饮品类型（旧数据兼容用；新记录的实际水种名见 WaterRecord.drinkName） */
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
