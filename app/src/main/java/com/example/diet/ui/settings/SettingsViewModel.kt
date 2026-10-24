package com.example.diet.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.BackupManager
import com.example.diet.data.remote.ApiKeyStore
import com.example.diet.data.remote.LLMManager
import com.example.diet.data.remote.provider.OpenAiProvider
import com.example.diet.data.remote.provider.VendorConfig
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.data.local.entity.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val profile: UserProfile = UserProfile(),
    val vendors: List<VendorConfig> = emptyList(),
    val selectedVendorId: String = "",
    val apiKey: String = "",
    val promptAnalyze: String = "",
    val promptSuggest: String = "",
    val message: String? = null,
    val testing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val llmManager: LLMManager,
    private val apiKeyStore: ApiKeyStore,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profileRepository.getProfile()
            val vendors = llmManager.vendors
            val selected = apiKeyStore.getSetting("selected_provider", vendors.first().id)
            val key = apiKeyStore.getApiKey(selected) ?: ""
            val promptAnalyze = apiKeyStore.getSetting("prompt_analyze", OpenAiProvider.DEFAULT_ANALYZE)
            val promptSuggest = apiKeyStore.getSetting("prompt_suggest", OpenAiProvider.DEFAULT_SUGGEST)
            _uiState.value = SettingsUiState(
                profile = profile, vendors = vendors, selectedVendorId = selected, apiKey = key,
                promptAnalyze = promptAnalyze, promptSuggest = promptSuggest
            )
        }
    }

    fun updateProfile(block: UserProfile.() -> UserProfile) {
        _uiState.update { it.copy(profile = it.profile.block()) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            profileRepository.saveProfile(_uiState.value.profile)
            _uiState.update { it.copy(message = "已保存") }
        }
    }

    fun selectVendor(id: String) {
        val key = apiKeyStore.getApiKey(id) ?: ""
        apiKeyStore.setSetting("selected_provider", id)
        _uiState.update { it.copy(selectedVendorId = id, apiKey = key) }
    }

    fun setApiKey(value: String) {
        apiKeyStore.setApiKey(_uiState.value.selectedVendorId, value)
        _uiState.update { it.copy(apiKey = value, message = "API Key 已保存（加密存储）") }
    }

    fun setPromptAnalyze(value: String) {
        apiKeyStore.setSetting("prompt_analyze", value)
        _uiState.update { it.copy(promptAnalyze = value) }
    }

    fun setPromptSuggest(value: String) {
        apiKeyStore.setSetting("prompt_suggest", value)
        _uiState.update { it.copy(promptSuggest = value) }
    }

    fun resetPrompts() {
        apiKeyStore.setSetting("prompt_analyze", OpenAiProvider.DEFAULT_ANALYZE)
        apiKeyStore.setSetting("prompt_suggest", OpenAiProvider.DEFAULT_SUGGEST)
        _uiState.update {
            it.copy(
                promptAnalyze = OpenAiProvider.DEFAULT_ANALYZE,
                promptSuggest = OpenAiProvider.DEFAULT_SUGGEST,
                message = "已恢复默认提示语"
            )
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(testing = true, message = null) }
            val result = llmManager.testConnection(_uiState.value.selectedVendorId)
            _uiState.update {
                it.copy(
                    testing = false,
                    message = result.fold(
                        onSuccess = { "连接成功" },
                        onFailure = { "连接失败：${it.message}" }
                    )
                )
            }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            val path = backupManager.export()
            _uiState.update { it.copy(message = "已导出：$path") }
        }
    }

    fun importData() {
        viewModelScope.launch {
            val result = backupManager.importLatest()
            _uiState.update {
                it.copy(message = result.fold(
                    onSuccess = { "已恢复备份：$it" },
                    onFailure = { "导入失败：${it.message}" }
                ))
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
