package com.example.diet.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.dao.DailyVolume
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.data.local.entity.WeightRecord
import com.example.diet.data.repository.AnalysisRepository
import com.example.diet.data.repository.MealRepository
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.data.repository.WaterRepository
import com.example.diet.data.repository.WeightRepository
import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AnalysisUiState(
    val meals: List<MealRecord> = emptyList(),
    val calorieIntake: Int = 0,
    val profile: UserProfile? = null,
    /** 近 30 天体重记录（每天只保留最后一条） */
    val weightTrend: List<WeightRecord> = emptyList(),
    /** 近 7 天每日饮水量（缺失日期补 0） */
    val waterWeek: List<DailyVolume> = emptyList(),
    val isLoading: Boolean = true
) {
    val proteinIntake: Float get() = meals.fold(0f) { acc, m -> acc + m.proteinG }
    val carbIntake: Float get() = meals.fold(0f) { acc, m -> acc + m.carbG }
    val fatIntake: Float get() = meals.fold(0f) { acc, m -> acc + m.fatG }

    /** 近 7 天日均饮水量（ml） */
    val waterAverage: Int
        get() = if (waterWeek.isEmpty()) 0 else waterWeek.sumOf { it.volumeMl } / waterWeek.size

    /** 近 7 天达标天数 */
    fun waterReachedDays(goalMl: Int): Int =
        if (goalMl <= 0) 0 else waterWeek.count { it.volumeMl >= goalMl }
}

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val profileRepository: ProfileRepository,
    private val analysisRepository: AnalysisRepository,
    private val weightRepository: WeightRepository,
    private val waterRepository: WaterRepository
) : ViewModel() {

    private val weightRangeStart = DateUtils.dateOf(LocalDate.now().minusDays(29))
    private val waterRangeStart = DateUtils.dateOf(LocalDate.now().minusDays(6))

    val uiState: StateFlow<AnalysisUiState> = combine(
        mealRepository.observeByDate(DateUtils.today()),
        mealRepository.observeCaloriesByDate(DateUtils.today()),
        profileRepository.observeProfile(),
        weightRepository.observeBetween(weightRangeStart, DateUtils.today()),
        waterRepository.observeDailyVolumesBetween(waterRangeStart, DateUtils.today())
    ) { meals, calories, profile, weights, water ->
        AnalysisUiState(
            meals = meals,
            calorieIntake = calories,
            profile = profile,
            weightTrend = weights
                .groupBy { it.date }
                .map { (_, sameDay) -> sameDay.maxByOrNull { r -> r.recordedAt } ?: sameDay.first() }
                .sortedBy { it.date },
            waterWeek = padWaterWeek(water),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState())

    private val _suggestions = MutableStateFlow<List<FoodSuggestion>>(emptyList())
    val suggestions: StateFlow<List<FoodSuggestion>> = _suggestions.asStateFlow()

    private val _suggestionLoading = MutableStateFlow(false)
    val suggestionLoading: StateFlow<Boolean> = _suggestionLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 记录今日体重，并同步资料中的当前体重 */
    fun logWeight(weightKg: Float) {
        if (weightKg <= 0f || weightKg > 400f) return
        viewModelScope.launch {
            weightRepository.addWeight(
                WeightRecord(date = DateUtils.today(), weightKg = weightKg)
            )
            profileRepository.updateCurrentWeight(weightKg)
        }
    }

    fun loadSuggestions() {
        viewModelScope.launch {
            _suggestionLoading.value = true
            _error.value = null
            val goal = uiState.value.profile?.dailyCalorieGoal ?: 1800
            val remaining = (goal - uiState.value.calorieIntake).coerceAtLeast(0)
            analysisRepository.suggestFoods(remaining, DateUtils.today())
                .onSuccess { _suggestions.value = it }
                .onFailure { _error.value = it.message ?: "获取建议失败" }
            _suggestionLoading.value = false
        }
    }

    fun clearError() = _error.update { null }

    /** 查询结果只包含有记录的日期，这里补齐最近 7 天，方便柱状图对齐 */
    private fun padWaterWeek(volumes: List<DailyVolume>): List<DailyVolume> {
        val byDate = volumes.associate { it.date to it.volumeMl }
        return (6 downTo 0).map { offset ->
            val date = DateUtils.dateOf(LocalDate.now().minusDays(offset.toLong()))
            DailyVolume(date = date, volumeMl = byDate[date] ?: 0)
        }
    }
}
