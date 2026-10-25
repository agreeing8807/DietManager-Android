package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 饮品配方：按每 100ml 的比例描述水分与营养，不依赖大模型即可估算。
 * waterRatio 为计入水分的比例（0~1），如牛奶 0.87 表示 200ml 牛奶算 174ml 水分。
 */
@Entity(tableName = "drink_preset")
@Serializable
data class DrinkPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val builtIn: Boolean = false,
    val waterRatio: Float = 1f,
    val caloriesPer100ml: Float = 0f,
    val proteinPer100ml: Float = 0f,
    val carbPer100ml: Float = 0f,
    val fatPer100ml: Float = 0f,
    val sortOrder: Int = 0
)

/** 内置水种（首次使用时写入数据库，可编辑比例、可删除） */
val DEFAULT_DRINK_PRESETS: List<DrinkPreset> = listOf(
    DrinkPreset(name = "纯净水", builtIn = true, waterRatio = 1f, sortOrder = 0),
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
    DrinkPreset(
        name = "蛋白粉", builtIn = true, waterRatio = 0.90f,
        caloriesPer100ml = 48f, proteinPer100ml = 9.6f,
        carbPer100ml = 2.0f, fatPer100ml = 0.8f, sortOrder = 3
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
