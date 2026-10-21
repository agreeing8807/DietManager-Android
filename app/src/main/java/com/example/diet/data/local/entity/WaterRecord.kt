package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.diet.domain.model.DrinkType
import kotlinx.serialization.Serializable

/**
 * 饮水记录。奶茶等含糖饮品同时计入热量与水分（isMilkTea=true）。
 */
@Entity(
    tableName = "water_record",
    indices = [Index(value = ["date"])]
)
@Serializable
data class WaterRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,            // yyyy-MM-dd
    val type: DrinkType,
    val volumeMl: Int,
    val caloriesKcal: Int,       // 纯水=0；奶茶/含糖>0
    val isMilkTea: Boolean,      // 同时计入热量与水分
    val recordedAt: Long = System.currentTimeMillis()
)
