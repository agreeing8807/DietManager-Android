package com.example.diet.data.repository

import com.example.diet.data.local.dao.UserProfileDao
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.domain.NutritionCalculator
import com.example.diet.domain.model.ActivityLevel
import com.example.diet.domain.model.Gender
import com.example.diet.util.AgeCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val dao: UserProfileDao
) {
    /** 始终返回非空资料（缺失时返回默认），年龄按生日实时计算 */
    fun observeProfile(): Flow<UserProfile> = dao.observe().map { (it ?: defaultProfile()).withComputedAge() }

    suspend fun getProfile(): UserProfile = (dao.get() ?: defaultProfile()).withComputedAge()

    suspend fun saveProfile(profile: UserProfile) {
        val p = profile.withComputedAge()
        val goal = if (p.dailyCalorieGoal <= 0) {
            NutritionCalculator.dailyCalorieGoal(
                p.gender, p.currentWeightKg, p.heightCm,
                p.age, p.activity, p.targetWeightKg
            )
        } else p.dailyCalorieGoal
        dao.upsert(p.copy(dailyCalorieGoal = goal, updatedAt = System.currentTimeMillis()))
    }

    private fun UserProfile.withComputedAge(): UserProfile =
        birthday?.let { copy(age = AgeCalculator.ageFromBirthday(it)) } ?: this

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
