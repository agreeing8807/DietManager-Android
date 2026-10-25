package com.example.diet.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.data.repository.AnalysisRepository
import com.example.diet.data.repository.MealRepository
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.domain.model.MealAnalysis
import com.example.diet.domain.model.MealType
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** 当前展示的日期与该日用餐记录 */
data class MealDayState(
    val date: String = "",
    val today: String = "",
    val meals: List<MealRecord> = emptyList(),
    val profile: UserProfile = UserProfile()
) {
    val totalCalories: Int get() = meals.sumOf { it.caloriesKcal }
    val proteinG: Float get() = meals.fold(0f) { a, m -> a + m.proteinG }
    val carbG: Float get() = meals.fold(0f) { a, m -> a + m.carbG }
    val fatG: Float get() = meals.fold(0f) { a, m -> a + m.fatG }
    val isToday: Boolean get() = date == today
}

data class MealUiState(
    val analyzing: Boolean = false,
    val message: String? = null,
    val suggestions: List<FoodSuggestion> = emptyList(),
    val suggesting: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val profileRepository: ProfileRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    /** 空串表示跟随“今天” */
    private val selectedDate = MutableStateFlow("")

    val dayState: StateFlow<MealDayState> =
        combine(profileRepository.observeProfile(), selectedDate) { profile, selected ->
            val today = DateUtils.today(profile.dayStartHour)
            Triple(profile, today, selected.ifBlank { today })
        }.flatMapLatest { (profile, today, date) ->
            mealRepository.observeByDate(date).map { meals ->
                MealDayState(
                    date = date,
                    today = today,
                    meals = meals.sortedBy { it.createdAt },
                    profile = profile
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MealDayState())

    private val _uiState = MutableStateFlow(MealUiState())
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    fun selectDate(date: String) {
        if (date.isBlank()) return
        val today = dayState.value.today
        if (today.isNotBlank() && date > today) return
        selectedDate.value = date
    }

    fun shiftDate(days: Long) {
        val current = dayState.value.date.ifBlank { dayState.value.today }
        val base = DateUtils.parse(current) ?: LocalDate.now()
        selectDate(DateUtils.dateOf(base.plusDays(days)))
    }

    fun backToToday() {
        selectedDate.value = ""
    }

    /**
     * 添加一餐：餐次由 UI 按系统时间自动判断并允许用户修改。
     * 记录日期固定为“今天”（按每日开始时间换算），与日历中正在查看的历史日期无关，
     * 从根上避免把记录写入往日或产生跨日重复。
     *
     * @return 是否受理本次提交（被拒绝时调用方不要清空输入框）
     */
    fun addMeal(type: MealType, description: String, imageBytes: ByteArray?): Boolean {
        if (description.isBlank() && imageBytes == null) return false
        // 防重复提交：分析过程中不再受理
        if (_uiState.value.analyzing) return false
        viewModelScope.launch {
            _uiState.update { it.copy(analyzing = true, message = null) }
            val date = DateUtils.today(dayState.value.profile.dayStartHour)
            val result = analysisRepository.analyzeMeal(description, imageBytes)
            val analysis: MealAnalysis? = result.getOrNull()
            val record = MealRecord(
                date = date,
                type = type,
                // 标题统一由模型综合图片与文字得出；分析失败时才回退到用户原文
                description = analysis?.title?.takeIf { it.isNotBlank() }
                    ?: description.ifBlank { "未识别餐食" },
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
            val failure = result.exceptionOrNull()?.message
            _uiState.update {
                it.copy(
                    analyzing = false,
                    message = when {
                        analysis != null -> "分析完成"
                        failure != null -> "已保存，但未完成分析：$failure"
                        else -> "已保存"
                    }
                )
            }
        }
        return true
    }

    fun updateMeal(record: MealRecord, type: MealType, description: String, calories: Int) {
        viewModelScope.launch {
            mealRepository.updateMeal(
                record.copy(
                    type = type,
                    description = description.ifBlank { record.description },
                    caloriesKcal = calories.coerceAtLeast(0)
                )
            )
        }
    }

    fun deleteMeal(record: MealRecord) {
        viewModelScope.launch { mealRepository.deleteMeal(record) }
    }

    /** “吃什么”建议：按当天剩余热量向大模型请求 */
    fun loadSuggestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(suggesting = true, error = null) }
            val day = dayState.value
            val goal = day.profile.dailyCalorieGoal
            val remaining = (goal - day.totalCalories).coerceAtLeast(0)
            analysisRepository.suggestFoods(remaining, day.date)
                .onSuccess { list -> _uiState.update { it.copy(suggestions = list, suggesting = false) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(suggesting = false, error = e.message ?: "获取建议失败")
                    }
                }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
