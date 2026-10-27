package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.diet.domain.model.DrinkKind
import kotlinx.serialization.Serializable

/**
 * 饮品配方，不依赖大模型即可估算。两种形态：
 * - [DrinkKind.LIQUID]：按每 100ml 描述营养；waterRatio 为计入水分的比例（0~1），
 *   如牛奶 0.87 表示 200ml 牛奶算 174ml 水分。
 * - [DrinkKind.POWDER]：冲调型，营养按「每份干粉（如一勺）」固定记录，
 *   实际冲调水量只影响总容量与浓度，不改变蛋白质等营养总量。
 */
@Entity(tableName = "drink_preset")
@Serializable
data class DrinkPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val builtIn: Boolean = false,
    val kind: DrinkKind = DrinkKind.LIQUID,
    val waterRatio: Float = 1f,
    val caloriesPer100ml: Float = 0f,
    val proteinPer100ml: Float = 0f,
    val carbPer100ml: Float = 0f,
    val fatPer100ml: Float = 0f,
    // ---- 冲调型专用：每份干粉的固定数据 ----
    /** 每份干粉重量（g），仅用于展示与估算冲调后体积 */
    val servingGrams: Float = 0f,
    val caloriesPerServing: Float = 0f,
    val proteinPerServing: Float = 0f,
    val carbPerServing: Float = 0f,
    val fatPerServing: Float = 0f,
    val sortOrder: Int = 0
)

/** 必须保留的基础水种：比例与营养可编辑，但不允许删除或改名 */
const val PURE_WATER_PRESET_NAME = "纯净水"

/** 内置水种（首次使用时写入数据库，可编辑比例、可删除；仅纯净水不可删改） */
val DEFAULT_DRINK_PRESETS: List<DrinkPreset> = listOf(
    DrinkPreset(name = PURE_WATER_PRESET_NAME, builtIn = true, waterRatio = 1f, sortOrder = 0),
    DrinkPreset(
        name = "牛奶", builtIn = true, waterRatio = 0.87f,
        caloriesPer100ml = 54f, proteinPer100ml = 3.0f,
        carbPer100ml = 4.8f, fatPer100ml = 3.2f, sortOrder = 1
    ),
    DrinkPreset(
        name = "奶茶", builtIn = true, waterRatio = 0.80f,
        caloriesPer100ml = 60f, proteinPer100ml = 0.6f,
        carbPer100ml = 11.5f, fatPer100ml = 1.6f, sortOrder = 2
    ),
    // 冲调型示例：一勺（约 30g）固定含 120 kcal / 蛋白 24g，冲多少水都不改变蛋白总量
    DrinkPreset(
        name = "蛋白粉（冲调）", builtIn = true, kind = DrinkKind.POWDER,
        servingGrams = 30f, caloriesPerServing = 120f, proteinPerServing = 24f,
        carbPerServing = 3f, fatPerServing = 1.5f, sortOrder = 3
    ),
    DrinkPreset(
        name = "果茶", builtIn = true, waterRatio = 0.95f,
        caloriesPer100ml = 40f, proteinPer100ml = 0.1f,
        carbPer100ml = 10f, fatPer100ml = 0.1f, sortOrder = 4
    ),
    DrinkPreset(
        name = "绿茶", builtIn = true, waterRatio = 0.99f,
        caloriesPer100ml = 1f, proteinPer100ml = 0f,
        carbPer100ml = 0.3f, fatPer100ml = 0f, sortOrder = 5
    )
)
