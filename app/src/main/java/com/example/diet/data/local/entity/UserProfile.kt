package com.example.diet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.diet.domain.model.ActivityLevel
import com.example.diet.domain.model.Gender
import kotlinx.serialization.Serializable

/**
 * 用户资料与目标，全局只有一行（id = 1）。
 */
@Entity(tableName = "user_profile")
@Serializable
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val gender: Gender = Gender.MALE,
    val age: Int = 25,
    val heightCm: Float = 170f,
    val currentWeightKg: Float = 65f,
    val targetWeightKg: Float = 60f,
    val activity: ActivityLevel = ActivityLevel.LIGHT,
    /** 每日热量目标（kcal），由计算器推导，可手动覆盖 */
    val dailyCalorieGoal: Int = 1800,
    /** 每日饮水目标（ml） */
    val waterGoalMl: Int = 2000,
    val updatedAt: Long = System.currentTimeMillis()
)
