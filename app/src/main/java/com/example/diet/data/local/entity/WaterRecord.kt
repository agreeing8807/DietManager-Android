package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.diet.domain.model.DrinkType
import kotlinx.serialization.Serializable

/**
 * 饮水记录。饮品按配方比例记录水分与营养，奶茶等含糖饮品同时计入热量。
 * waterMl 为实际计入水分的量（旧数据为 0，按 volumeMl 兜底计算）。
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
    val volumeMl: Int,           // 实际饮用量
    val waterMl: Int = 0,        // 计入水分的量；0 表示按 volumeMl 计
    val drinkName: String = "",  // 水种名称（自定义/内置）
    val caloriesKcal: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    val isMilkTea: Boolean = false,
    val recordedAt: Long = System.currentTimeMillis()
)
