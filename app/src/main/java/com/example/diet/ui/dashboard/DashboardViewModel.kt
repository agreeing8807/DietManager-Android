package com.example.diet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.local.entity.QuickItem
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.data.local.entity.WaterRecord
import com.example.diet.data.local.entity.WeightRecord
import com.example.diet.data.repository.MealRepository
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.data.repository.QuickItemRepository
import com.example.diet.data.repository.WaterRepository
import com.example.diet.data.repository.WeightRepository
import com.example.diet.domain.model.DrinkType
import com.example.diet.domain.model.MealType
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class DashboardUiState(
    val profile: UserProfile = UserProfile(),
    val date: String = "",
    val calorieIntake: Int = 0,
    val waterMl: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    /** 近 30 天体重记录（每天取最后一条） */
    val weightTrend: List<WeightRecord> = emptyList(),
    val quickItems: List<QuickItem> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository,
    private val quickItemRepository: QuickItemRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = profileRepository.observeProfile()
        .flatMapLatest { profile ->
            val dayStart = profile.dayStartHour
            val date = DateUtils.today(dayStart)
            val start = DateUtils.dateOf(DateUtils.effectiveDate(dayStart).minusDays(29))
            combine(
                mealRepository.observeByDate(date),
                waterRepository.observeByDate(date),
                weightRepository.observeBetween(start, date),
                quickItemRepository.observeAll()
            ) { meals, drinks, weights, quickItems ->
                DashboardUiState(
                    profile = profile,
                    date = date,
                    calorieIntake = meals.sumOf { it.caloriesKcal } + drinks.sumOf { it.caloriesKcal },
                    waterMl = drinks.sumOf { if (it.waterMl > 0) it.waterMl else it.volumeMl },
                    proteinG = meals.fold(0f) { acc, m -> acc + m.proteinG } +
                        drinks.fold(0f) { acc, d -> acc + d.proteinG },
                    carbG = meals.fold(0f) { acc, m -> acc + m.carbG } +
                        drinks.fold(0f) { acc, d -> acc + d.carbG },
                    fatG = meals.fold(0f) { acc, m -> acc + m.fatG } +
                        drinks.fold(0f) { acc, d -> acc + d.fatG },
                    weightTrend = weights
                        .groupBy { it.date }
                        .map { (_, sameDay) -> sameDay.maxByOrNull { r -> r.recordedAt } ?: sameDay.first() }
                        .sortedBy { it.date },
                    quickItems = quickItems,
                    isLoading = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    /** 记录体重，并同步资料里的当前体重 */
    fun logWeight(weightKg: Float) {
        if (weightKg <= 0f || weightKg > 400f) return
        viewModelScope.launch {
            val date = DateUtils.today(uiState.value.profile.dayStartHour)
            weightRepository.addWeight(WeightRecord(date = date, weightKg = weightKg))
            profileRepository.updateCurrentWeight(weightKg)
        }
    }

    /**
     * 一键记录常用饮食：热量与营养计入当天饮食，水分计入当天饮水。
     * 水分记录热量置 0，避免与饮食记录重复计数。
     */
    fun applyQuickItem(item: QuickItem) {
        viewModelScope.launch {
            val profile = uiState.value.profile
            val date = DateUtils.today(profile.dayStartHour)
            val hasNutrition = item.caloriesKcal > 0 || item.proteinG > 0f ||
                item.carbG > 0f || item.fatG > 0f
            if (hasNutrition) {
                mealRepository.addMeal(
                    MealRecord(
                        date = date,
                        type = MealType.fromHour(LocalTime.now().hour),
                        description = item.name,
                        caloriesKcal = item.caloriesKcal,
                        proteinG = item.proteinG,
                        carbG = item.carbG,
                        fatG = item.fatG,
                        summary = "快捷记录",
                        analyzedByAi = false
                    )
                )
            }
            if (item.waterMl > 0) {
                waterRepository.addWater(
                    WaterRecord(
                        date = date,
                        type = DrinkType.OTHER,
                        volumeMl = item.waterMl,
                        waterMl = item.waterMl,
                        drinkName = item.name,
                        caloriesKcal = 0,
                        isMilkTea = false
                    )
                )
            }
        }
    }

    fun saveQuickItem(item: QuickItem) {
        viewModelScope.launch {
            if (item.id == 0L) quickItemRepository.add(item) else quickItemRepository.update(item)
        }
    }

    fun deleteQuickItem(item: QuickItem) {
        viewModelScope.launch { quickItemRepository.delete(item) }
    }
}
