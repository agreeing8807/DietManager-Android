package com.example.diet.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    init {
        viewModelScope.launch { _profile.value = profileRepository.getProfile() }
    }

    fun update(block: UserProfile.() -> UserProfile) {
        _profile.update { it.block() }
    }

    /** 保存资料并回调（回调在保存完成后执行）；引导页强制按填写的资料重算热量目标 */
    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            profileRepository.saveProfile(_profile.value.copy(dailyCalorieGoal = 0))
            onDone()
        }
    }
}
