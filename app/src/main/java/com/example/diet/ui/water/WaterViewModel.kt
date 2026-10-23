package com.example.diet.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.entity.WaterRecord
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.data.repository.WaterRepository
import com.example.diet.domain.NutritionCalculator
import com.example.diet.domain.model.DrinkType
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterUiState(
    val records: List<WaterRecord> = emptyList(),
    val volume: Int = 0,
    val calories: Int = 0,
    val profile: UserProfile? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val uiState: StateFlow<WaterUiState> = combine(
        waterRepository.observeByDate(DateUtils.today()),
        waterRepository.observeVolumeByDate(DateUtils.today()),
        waterRepository.observeCaloriesByDate(DateUtils.today()),
        profileRepository.observeProfile()
    ) { records, volume, calories, profile ->
        WaterUiState(
            records = records,
            volume = volume,
            calories = calories,
            profile = profile,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WaterUiState()
    )

    fun addDrink(type: DrinkType, volumeMl: Int, explicitCalories: Int? = null) {
        viewModelScope.launch {
            val (cal, isMilkTea) = NutritionCalculator.estimateDrink(type, volumeMl, explicitCalories)
            waterRepository.addWater(
                WaterRecord(
                    date = DateUtils.today(),
                    type = type,
                    volumeMl = volumeMl,
                    caloriesKcal = cal,
                    isMilkTea = isMilkTea
                )
            )
        }
    }

    fun delete(record: WaterRecord) {
        viewModelScope.launch { waterRepository.deleteWater(record) }
    }
}
