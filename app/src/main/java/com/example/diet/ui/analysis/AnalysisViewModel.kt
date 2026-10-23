package com.example.diet.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.repository.AnalysisRepository
import com.example.diet.data.repository.MealRepository
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisUiState(
    val meals: List<MealRecord> = emptyList(),
    val calorieIntake: Int = 0,
    val profile: UserProfile? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val profileRepository: ProfileRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    val uiState: StateFlow<AnalysisUiState> = combine(
        mealRepository.observeByDate(DateUtils.today()),
        mealRepository.observeCaloriesByDate(DateUtils.today()),
        profileRepository.observeProfile()
    ) { meals, calories, profile ->
        AnalysisUiState(meals = meals, calorieIntake = calories, profile = profile, isLoading = false)
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), AnalysisUiState())

    private val _suggestions = MutableStateFlow<List<FoodSuggestion>>(emptyList())
    val suggestions: StateFlow<List<FoodSuggestion>> = _suggestions.asStateFlow()

    private val _suggestionLoading = MutableStateFlow(false)
    val suggestionLoading: StateFlow<Boolean> = _suggestionLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
}
