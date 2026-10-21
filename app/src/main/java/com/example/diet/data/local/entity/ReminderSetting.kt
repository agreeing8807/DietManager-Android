package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 喝水提醒时间点设置（可自定义增删）。
 * timeMinutesOfDay: 当天分钟数，如 480 = 08:00
 */
@Entity(tableName = "reminder_setting")
@Serializable
data class ReminderSetting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMinutesOfDay: Int,
    val label: String = "喝水提醒",
    val enabled: Boolean = true
)
