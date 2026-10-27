package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.diet.domain.model.DataStatus
import kotlinx.serialization.Serializable

/**
 * 一餐中的单项食物。是营养数据的真源：MealRecord 上的四个总量只是它的缓存快照。
 *
 * 只保存「每 100g 基准 + 克数」，绝对值由 [com.example.diet.domain.MealItems] 换算，
 * 这样用户改克数时不需要重新调用任何模型。
 */
@Entity(
    tableName = "meal_item",
    indices = [Index(value = ["recordId"])],
    foreignKeys = [
        ForeignKey(
            entity = MealRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class MealItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val name: String,
    val brand: String = "",
    /** 实际食用克数（饮品按 ml 近似） */
    val grams: Float = 0f,
    /** 可数单位的份数（1 个鸡蛋 / 2 个包子 / 1 勺），0 表示按克记录 */
    val servings: Float = 0f,
    /**
     * 该条目带来的水分（ml）。饮品用：如一杯 500ml 奶茶约 400ml 水分。
     * 0 表示不含水分，按 0 计入，不影响饮水统计。
     */
    val waterMl: Float = 0f,
    // ---- 每 100g 基准，来自内置成分表或模型补数 ----
    val kcalPer100g: Float = 0f,
    val proteinPer100g: Float = 0f,
    val carbPer100g: Float = 0f,
    val fatPer100g: Float = 0f,
    /** 数据来源，如「内置食物成分表」「模型补充」 */
    val source: String = "",
    /** 0~1，来自补数模型的可靠程度；本地成分为 1 */
    val confidence: Float = 0f,
    val status: DataStatus = DataStatus.ESTIMATED,
    val sortOrder: Int = 0
) {
    /** 是否有可用的每 100g 基准，决定能否按克数换算 */
    val hasBasis: Boolean
        get() = kcalPer100g > 0f || proteinPer100g > 0f ||
            carbPer100g > 0f || fatPer100g > 0f
}
