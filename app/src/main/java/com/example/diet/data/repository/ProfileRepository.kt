package com.example.diet.data.repository

import com.example.diet.data.local.dao.UserProfileDao
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.domain.NutritionCalculator
import com.example.diet.domain.model.ActivityLevel
import com.example.diet.domain.model.Gender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val dao: UserProfileDao
) {
    /** 始终返回非空资料（缺失时返回默认并落库） */
    fun observeProfile(): Flow<UserProfile> = dao.observe().map { it ?: defaultProfile() }

    suspend fun getProfile(): UserProfile = dao.get() ?: defaultProfile()

    suspend fun saveProfile(profile: UserProfile) {
        val goal = if (profile.dailyCalorieGoal <= 0) {
            NutritionCalculator.dailyCalorieGoal(
                profile.gender, profile.currentWeightKg, profile.heightCm,
                profile.age, profile.activity, profile.targetWeightKg
            )
        } else profile.dailyCalorieGoal
        dao.upsert(profile.copy(dailyCalorieGoal = goal, updatedAt = System.currentTimeMillis()))
    }

    private fun defaultProfile(): UserProfile {
        val goal = NutritionCalculator.dailyCalorieGoal(
            Gender.MALE, 65f, 170f, 25, ActivityLevel.LIGHT, 60f
        )
        return UserProfile(
            gender = Gender.MALE, age = 25, heightCm = 170f,
            currentWeightKg = 65f, targetWeightKg = 60f,
            activity = ActivityLevel.LIGHT, dailyCalorieGoal = goal, waterGoalMl = 2000
        )
    }
}
