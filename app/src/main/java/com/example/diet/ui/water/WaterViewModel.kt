package com.example.diet.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.dao.DailyVolume
import com.example.diet.data.local.entity.DrinkPreset
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.data.local.entity.WaterRecord
import com.example.diet.data.repository.DrinkPresetRepository
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.data.repository.WaterRepository
import com.example.diet.domain.DrinkCalculator
import com.example.diet.domain.model.DrinkType
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterUiState(
    val profile: UserProfile = UserProfile(),
    val date: String = "",
    val presets: List<DrinkPreset> = emptyList(),
    val todayRecords: List<WaterRecord> = emptyList(),
    val todayWaterMl: Int = 0,
    val todayCalories: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    /** 往日每日汇总（不含今天），按日期倒序 */
    val pastDays: List<DailyVolume> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WaterViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
    private val profileRepository: ProfileRepository,
    private val presetRepository: DrinkPresetRepository
) : ViewModel() {

    val uiState: StateFlow<WaterUiState> = profileRepository.observeProfile()
        .flatMapLatest { profile ->
            val dayStart = profile.dayStartHour
            val date = DateUtils.today(dayStart)
            val start = DateUtils.dateOf(DateUtils.effectiveDate(dayStart).minusDays(13))
            combine(
                presetRepository.observeAll(),
                waterRepository.observeByDate(date),
                waterRepository.observeDailyTotalsBetween(start, date)
            ) { presets, records, daily ->
                WaterUiState(
                    profile = profile,
                    date = date,
                    presets = presets,
                    todayRecords = records,
                    todayWaterMl = records.fold(0) { acc, r -> acc + if (r.waterMl > 0) r.waterMl else r.volumeMl },
                    todayCalories = records.fold(0) { acc, r -> acc + r.caloriesKcal },
                    proteinG = records.fold(0f) { acc, r -> acc + r.proteinG },
                    carbG = records.fold(0f) { acc, r -> acc + r.carbG },
                    fatG = records.fold(0f) { acc, r -> acc + r.fatG },
                    pastDays = daily.filter { it.date != date },
                    isLoading = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaterUiState())

    init {
        viewModelScope.launch { presetRepository.ensureDefaults() }
    }

    /** 按配方比例计算并记录一次饮用，不调用大模型 */
    fun addDrink(preset: DrinkPreset, volumeMl: Int) {
        if (volumeMl <= 0) return
        viewModelScope.launch {
            val date = DateUtils.today(uiState.value.profile.dayStartHour)
            val nutrition = DrinkCalculator.nutritionOf(preset, volumeMl)
            waterRepository.addWater(
                WaterRecord(
                    date = date,
                    type = typeOf(preset),
                    volumeMl = volumeMl,
                    waterMl = nutrition.waterMl,
                    drinkName = preset.name,
                    caloriesKcal = nutrition.caloriesKcal,
                    proteinG = nutrition.proteinG,
                    carbG = nutrition.carbG,
                    fatG = nutrition.fatG,
                    isMilkTea = preset.name == "奶茶"
                )
            )
        }
    }

    /** 修改今天的某条记录（水种与容量可改） */
    fun updateDrink(record: WaterRecord, preset: DrinkPreset, volumeMl: Int) {
        if (volumeMl <= 0) return
        viewModelScope.launch {
            val nutrition = DrinkCalculator.nutritionOf(preset, volumeMl)
            waterRepository.updateWater(
                record.copy(
                    type = typeOf(preset),
                    volumeMl = volumeMl,
                    waterMl = nutrition.waterMl,
                    drinkName = preset.name,
                    caloriesKcal = nutrition.caloriesKcal,
                    proteinG = nutrition.proteinG,
                    carbG = nutrition.carbG,
                    fatG = nutrition.fatG,
                    isMilkTea = preset.name == "奶茶"
                )
            )
        }
    }

    fun deleteDrink(record: WaterRecord) {
        viewModelScope.launch { waterRepository.deleteWater(record) }
    }

    /** 自定义水种：按每 100ml 填写比例 */
    fun addCustomPreset(
        name: String,
        waterRatioPercent: Int,
        caloriesPer100ml: Float,
        proteinPer100ml: Float,
        carbPer100ml: Float,
        fatPer100ml: Float
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val order = uiState.value.presets.size
            presetRepository.add(
                DrinkPreset(
                    name = name.trim(),
                    builtIn = false,
                    waterRatio = (waterRatioPercent.coerceIn(0, 100)) / 100f,
                    caloriesPer100ml = caloriesPer100ml,
                    proteinPer100ml = proteinPer100ml,
                    carbPer100ml = carbPer100ml,
                    fatPer100ml = fatPer100ml,
                    sortOrder = order
                )
            )
        }
    }

    fun deletePreset(preset: DrinkPreset) {
        viewModelScope.launch { presetRepository.delete(preset) }
    }

    private fun typeOf(preset: DrinkPreset): DrinkType = when (preset.name) {
        "纯净水" -> DrinkType.WATER
        "绿茶" -> DrinkType.TEA
        "奶茶" -> DrinkType.MILK_TEA
        else -> DrinkType.OTHER
    }
}
