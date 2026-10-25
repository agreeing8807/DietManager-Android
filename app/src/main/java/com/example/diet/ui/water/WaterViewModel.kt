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
import com.example.diet.domain.DayLedger
import com.example.diet.domain.DrinkCalculator
import com.example.diet.domain.model.DrinkKind
import com.example.diet.domain.model.DrinkType
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** 展开查看的往日明细（往日列表本身只有每日汇总） */
data class PastDayDetail(
    val date: String,
    val records: List<WaterRecord> = emptyList()
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
                val intake = DayLedger.of(emptyList(), records)
                WaterUiState(
                    profile = profile,
                    date = date,
                    presets = presets,
                    todayRecords = records,
                    todayWaterMl = intake.waterMl,
                    todayCalories = intake.caloriesKcal,
                    proteinG = intake.proteinG,
                    carbG = intake.carbG,
                    fatG = intake.fatG,
                    pastDays = daily.filter { it.date != date },
                    isLoading = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaterUiState())

    private val _pastDayDetail = MutableStateFlow<PastDayDetail?>(null)
    val pastDayDetail: StateFlow<PastDayDetail?> = _pastDayDetail.asStateFlow()

    init {
        viewModelScope.launch { presetRepository.ensureDefaults() }
    }

    /** 展开/收起往日某一天；往日列表本身只有汇总，展开时才读取明细 */
    fun togglePastDay(date: String) {
        if (_pastDayDetail.value?.date == date) {
            _pastDayDetail.value = null
            return
        }
        viewModelScope.launch {
            _pastDayDetail.value = PastDayDetail(date, waterRepository.getByDate(date))
        }
    }

    /** 修改/删除后刷新已展开的往日明细 */
    private suspend fun refreshPastDay(date: String) {
        val current = _pastDayDetail.value ?: return
        if (current.date != date) return
        _pastDayDetail.value = current.copy(records = waterRepository.getByDate(date))
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
            refreshPastDay(record.date)
        }
    }

    /**
     * 冲调型记录：营养按份数固定，加水量只决定总容量与浓度。
     * 干粉体积按 1g≈1ml 计入总容量；加水量全额计入当日饮水。
     */
    fun addMixedDrink(preset: DrinkPreset, servings: Float, waterMl: Int) {
        if (servings <= 0f || waterMl <= 0) return
        viewModelScope.launch {
            val date = DateUtils.today(uiState.value.profile.dayStartHour)
            val mix = DrinkCalculator.mixOf(preset, servings, waterMl)
            waterRepository.addWater(
                WaterRecord(
                    date = date,
                    type = typeOf(preset),
                    volumeMl = mix.totalMl,
                    waterMl = mix.waterMl,
                    drinkName = preset.name,
                    caloriesKcal = mix.caloriesKcal,
                    proteinG = mix.proteinG,
                    carbG = mix.carbG,
                    fatG = mix.fatG,
                    servings = mix.servings,
                    powderGrams = mix.powderGrams
                )
            )
        }
    }

    /** 修改冲调记录（份数与加水量可改） */
    fun updateMixedDrink(record: WaterRecord, preset: DrinkPreset, servings: Float, waterMl: Int) {
        if (servings <= 0f || waterMl <= 0) return
        viewModelScope.launch {
            val mix = DrinkCalculator.mixOf(preset, servings, waterMl)
            waterRepository.updateWater(
                record.copy(
                    type = typeOf(preset),
                    volumeMl = mix.totalMl,
                    waterMl = mix.waterMl,
                    drinkName = preset.name,
                    caloriesKcal = mix.caloriesKcal,
                    proteinG = mix.proteinG,
                    carbG = mix.carbG,
                    fatG = mix.fatG,
                    servings = mix.servings,
                    powderGrams = mix.powderGrams
                )
            )
            refreshPastDay(record.date)
        }
    }

    fun deleteDrink(record: WaterRecord) {
        viewModelScope.launch {
            waterRepository.deleteWater(record)
            refreshPastDay(record.date)
        }
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
                    caloriesPer100ml = caloriesPer100ml.coerceAtLeast(0f),
                    proteinPer100ml = proteinPer100ml.coerceAtLeast(0f),
                    carbPer100ml = carbPer100ml.coerceAtLeast(0f),
                    fatPer100ml = fatPer100ml.coerceAtLeast(0f),
                    sortOrder = order
                )
            )
        }
    }

    /** 冲调型水种：营养按每份干粉填写，与冲调水量无关 */
    fun addPowderPreset(
        name: String,
        servingGrams: Float,
        caloriesPerServing: Float,
        proteinPerServing: Float,
        carbPerServing: Float,
        fatPerServing: Float
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val order = uiState.value.presets.size
            presetRepository.add(
                DrinkPreset(
                    name = name.trim(),
                    builtIn = false,
                    kind = DrinkKind.POWDER,
                    servingGrams = servingGrams.coerceAtLeast(0f),
                    caloriesPerServing = caloriesPerServing.coerceAtLeast(0f),
                    proteinPerServing = proteinPerServing.coerceAtLeast(0f),
                    carbPerServing = carbPerServing.coerceAtLeast(0f),
                    fatPerServing = fatPerServing.coerceAtLeast(0f),
                    sortOrder = order
                )
            )
        }
    }

    /** 修改水种：按液体型保存，允许从冲调型改过来 */
    fun updateCustomPreset(
        preset: DrinkPreset,
        name: String,
        waterRatioPercent: Int,
        caloriesPer100ml: Float,
        proteinPer100ml: Float,
        carbPer100ml: Float,
        fatPer100ml: Float
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            presetRepository.update(
                preset.copy(
                    name = name.trim(),
                    kind = DrinkKind.LIQUID,
                    waterRatio = (waterRatioPercent.coerceIn(0, 100)) / 100f,
                    caloriesPer100ml = caloriesPer100ml.coerceAtLeast(0f),
                    proteinPer100ml = proteinPer100ml.coerceAtLeast(0f),
                    carbPer100ml = carbPer100ml.coerceAtLeast(0f),
                    fatPer100ml = fatPer100ml.coerceAtLeast(0f),
                    // 改成液体型后清掉冲调型字段，避免旧数据残留
                    servingGrams = 0f,
                    caloriesPerServing = 0f,
                    proteinPerServing = 0f,
                    carbPerServing = 0f,
                    fatPerServing = 0f
                )
            )
        }
    }

    /** 修改水种：按冲调型保存，允许从液体型改过来 */
    fun updatePowderPreset(
        preset: DrinkPreset,
        name: String,
        servingGrams: Float,
        caloriesPerServing: Float,
        proteinPerServing: Float,
        carbPerServing: Float,
        fatPerServing: Float
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            presetRepository.update(
                preset.copy(
                    name = name.trim(),
                    kind = DrinkKind.POWDER,
                    waterRatio = 1f,
                    caloriesPer100ml = 0f,
                    proteinPer100ml = 0f,
                    carbPer100ml = 0f,
                    fatPer100ml = 0f,
                    servingGrams = servingGrams.coerceAtLeast(0f),
                    caloriesPerServing = caloriesPerServing.coerceAtLeast(0f),
                    proteinPerServing = proteinPerServing.coerceAtLeast(0f),
                    carbPerServing = carbPerServing.coerceAtLeast(0f),
                    fatPerServing = fatPerServing.coerceAtLeast(0f)
                )
            )
        }
    }

    fun deletePreset(preset: DrinkPreset) {
        viewModelScope.launch { presetRepository.delete(preset) }
    }

    private fun typeOf(preset: DrinkPreset): DrinkType = when {
        preset.kind == DrinkKind.POWDER -> DrinkType.POWDER
        preset.name == "纯净水" -> DrinkType.WATER
        preset.name == "绿茶" -> DrinkType.TEA
        preset.name == "奶茶" -> DrinkType.MILK_TEA
        else -> DrinkType.OTHER
    }
}
