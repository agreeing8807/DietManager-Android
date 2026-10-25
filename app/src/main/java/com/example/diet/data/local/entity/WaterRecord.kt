package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.diet.domain.model.DrinkType
import kotlinx.serialization.Serializable

/**
 * 饮水记录。饮品按配方比例记录水分与营养，奶茶等含糖饮品同时计入热量。
 * waterMl 为实际计入水分的量（旧数据为 0，按 volumeMl 兜底计算）。
 *
 * 冲调型（servings > 0）：volumeMl 为冲调后的总容量，waterMl 为实际加水量，
 * 营养数值已按份数换算完成 —— 水量多少不改变蛋白质等总量。
 */
@Entity(
    tableName = "water_record",
    indices = [Index(value = ["date"])]
)
@Serializable
data class WaterRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,            // yyyy-MM-dd（按用户自定义的每日开始时间归属）
    val type: DrinkType,
    val volumeMl: Int,           // 实际饮用量（冲调型为加水量+干粉体积的总容量）
    val waterMl: Int = 0,        // 计入水分的量；0 表示按 volumeMl 计
    val drinkName: String = "",  // 水种名称（自定义/内置）
    val caloriesKcal: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    val isMilkTea: Boolean = false,
    /** 冲调型：干粉份数（一勺=1 份）；0 表示非冲调记录 */
    val servings: Float = 0f,
    /** 冲调型：本次干粉总重（份数 × 每份克数），仅用于展示 */
    val powderGrams: Float = 0f,
    val recordedAt: Long = System.currentTimeMillis()
)
