package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.diet.domain.model.DataStatus
import com.example.diet.domain.model.MealType
import kotlinx.serialization.Serializable

/**
 * 三餐/加餐记录。图片仅用于分析，不持久化（imagePath 始终为空或占位）。
 * [status] 明确这条记录的数据可信度，界面不再通过 summary 文案推断。
 */
@Entity(
    tableName = "meal_record",
    indices = [Index(value = ["date", "type"])]
)
@Serializable
data class MealRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,            // yyyy-MM-dd
    val type: MealType,
    val description: String,     // 用户文字描述或菜品名
    val caloriesKcal: Int,
    val proteinG: Float,
    val carbG: Float,
    val fatG: Float,
    val summary: String,         // 大模型分析摘要
    val analyzedByAi: Boolean = false,
    val status: DataStatus = DataStatus.ESTIMATED,
    val createdAt: Long = System.currentTimeMillis()
)
