package com.example.diet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.entity.WaterRecord
import com.example.diet.data.repository.MealRepository
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.data.repository.WaterRepository
import com.example.diet.data.repository.WeightRepository
import com.example.diet.domain.model.DrinkType
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val profile: UserProfile? = null,
    val calorieIntake: Int = 0,
    val waterVolume: Int = 0,
    val weightKg: Float? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        profileRepository.observeProfile(),
        mealRepository.observeCaloriesByDate(DateUtils.today()),
        waterRepository.observeVolumeByDate(DateUtils.today()),
        weightRepository.observeByDate(DateUtils.today())
    ) { profile, calories, water, weight ->
        DashboardUiState(
            profile = profile,
            calorieIntake = calories,
            waterVolume = water,
            weightKg = weight?.weightKg,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    suspend fun quickAddWater(ml: Int) {
        waterRepository.addWater(
            WaterRecord(
                date = DateUtils.today(),
                type = DrinkType.WATER,
                volumeMl = ml,
                caloriesKcal = 0,
                isMilkTea = false
            )
        )
    }
}
