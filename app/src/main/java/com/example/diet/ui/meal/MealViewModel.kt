package com.example.diet.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.repository.AnalysisRepository
import com.example.diet.data.repository.MealRepository
import com.example.diet.domain.model.MealAnalysis
import com.example.diet.domain.model.MealType
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MealUiState(
    val analyzing: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class MealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val selectedType = MutableStateFlow(MealType.BREAKFAST)

    val meals: StateFlow<List<MealRecord>> = selectedType.flatMapLatest { type ->
        mealRepository.observeByDate(DateUtils.today()).map { list -> list.filter { it.type == type } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(MealUiState())
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    fun setType(type: MealType) { selectedType.value = type }

    fun addMeal(type: MealType, description: String, imageBytes: ByteArray?) {
        if (description.isBlank() && imageBytes == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(analyzing = true, message = null) }
            val result = analysisRepository.analyzeMeal(description, imageBytes)
            val analysis: MealAnalysis? = result.getOrNull()
            val record = MealRecord(
                date = DateUtils.today(),
                type = type,
                description = description.ifBlank { "图片餐食" },
                caloriesKcal = analysis?.caloriesKcal ?: 0,
                proteinG = analysis?.proteinG ?: 0f,
                carbG = analysis?.carbG ?: 0f,
                fatG = analysis?.fatG ?: 0f,
                summary = analysis?.summary
                    ?: result.exceptionOrNull()?.message?.let { "未能自动分析：$it（已按手动记录保存）" }
                    ?: "已记录（未分析）",
                analyzedByAi = analysis != null
            )
            mealRepository.addMeal(record)
            _uiState.update {
                it.copy(
                    analyzing = false,
                    message = if (analysis != null) "分析完成" else "已保存（离线/未配置分析）"
                )
            }
        }
    }

    fun deleteMeal(record: MealRecord) {
        viewModelScope.launch { mealRepository.deleteMeal(record) }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
