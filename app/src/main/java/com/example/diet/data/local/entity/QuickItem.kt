package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 快捷吃喝项目：用户自定义的常用饮食，按钮只显示名称，一键记录。
 * 热量/蛋白质/碳水/脂肪计入当天饮食；水分计入当天饮水。
 */
@Entity(tableName = "quick_item")
@Serializable
data class QuickItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val caloriesKcal: Int = 0,
    val waterMl: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
