package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 体重记录，按日期维度。
 */
@Entity(
    tableName = "weight_record",
    indices = [Index(value = ["date"])]
)
@Serializable
data class WeightRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,            // yyyy-MM-dd
    val weightKg: Float,
    val recordedAt: Long = System.currentTimeMillis()
)
