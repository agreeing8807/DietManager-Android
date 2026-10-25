package com.example.diet.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.BackupManager
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.data.remote.ApiEntry
import com.example.diet.data.remote.ApiKeyStore
import com.example.diet.data.remote.LLMManager
import com.example.diet.data.remote.provider.OpenAiProvider
import com.example.diet.data.remote.provider.VendorConfig
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.domain.NutritionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ApiTestResult(val label: String, val success: Boolean, val detail: String)

data class SettingsUiState(
    val profile: UserProfile = UserProfile(),
    val vendors: List<VendorConfig> = emptyList(),
    val entries: List<ApiEntry> = emptyList(),
    // 新增 API 表单
    val newVendorId: String = "",
    val newModel: String = "",
    val newApiKey: String = "",
    val promptAnalyze: String = "",
    val promptSuggest: String = "",
    val message: String? = null,
    val adding: Boolean = false,
    val testing: Boolean = false,
    val testResults: List<ApiTestResult> = emptyList()
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
            val first = vendors.first()
            val promptAnalyze = apiKeyStore.getSetting("prompt_analyze", OpenAiProvider.DEFAULT_ANALYZE)
            val promptSuggest = apiKeyStore.getSetting("prompt_suggest", OpenAiProvider.DEFAULT_SUGGEST)
            _uiState.value = SettingsUiState(
                profile = profile,
                vendors = vendors,
                entries = apiKeyStore.getEntries(),
                newVendorId = first.id,
                newModel = first.defaultModel,
                promptAnalyze = promptAnalyze,
                promptSuggest = promptSuggest
            )
        }
    }

    // ---------------- 个人资料 ----------------

    fun updateProfile(block: UserProfile.() -> UserProfile) {
        _uiState.update { it.copy(profile = it.profile.block()) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            profileRepository.saveProfile(_uiState.value.profile)
            _uiState.update { it.copy(message = "个人资料已保存") }
        }
    }

    /** 按当前体重/活动量重新推算每日热量目标并保存 */
    fun recalcCalorieGoal() {
        _uiState.update { s ->
            val p = s.profile
            s.copy(
                profile = p.copy(
                    dailyCalorieGoal = NutritionCalculator.dailyCalorieGoal(
                        p.gender, p.currentWeightKg, p.heightCm, p.age, p.activity, p.targetWeightKg
                    )
                )
            )
        }
        saveProfile()
    }

    // ---------------- API 管理 ----------------

    fun selectNewVendor(id: String) {
        val vendor = llmManager.getVendor(id)
        _uiState.update { it.copy(newVendorId = id, newModel = vendor.defaultModel) }
    }

    fun setNewModel(value: String) = _uiState.update { it.copy(newModel = value) }
    fun setNewApiKey(value: String) = _uiState.update { it.copy(newApiKey = value) }

    /** 添加 API：先测试连接，成功才保存 */
    fun addApi() {
        val s = _uiState.value
        val key = s.newApiKey.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(message = "请填写 API Key") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(adding = true, message = null) }
            val entry = ApiEntry(
                id = UUID.randomUUID().toString(),
                vendorId = s.newVendorId,
                model = s.newModel.ifBlank { llmManager.getVendor(s.newVendorId).defaultModel },
                apiKey = key
            )
            val result = llmManager.testEntry(entry)
            if (result.isSuccess) {
                apiKeyStore.addEntry(entry)
                _uiState.update {
                    it.copy(
                        adding = false,
                        entries = apiKeyStore.getEntries(),
                        newApiKey = "",
                        message = "添加成功（${llmManager.getVendor(entry.vendorId).name}）"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        adding = false,
                        message = "添加失败：${result.exceptionOrNull()?.message ?: "无法连接"}"
                    )
                }
            }
        }
    }

    fun deleteApi(entry: ApiEntry) {
        apiKeyStore.deleteEntry(entry.id)
        _uiState.update { it.copy(entries = apiKeyStore.getEntries(), message = "已删除") }
    }

    /** 测试全部 API 连接并给出每条状态 */
    fun testAll() {
        viewModelScope.launch {
            val entries = apiKeyStore.getEntries()
            if (entries.isEmpty()) {
                _uiState.update { it.copy(message = "还没有添加任何 API") }
                return@launch
            }
            _uiState.update { it.copy(testing = true, message = null, testResults = emptyList()) }
            val results = entries.map { entry ->
                val r = llmManager.testEntry(entry)
                ApiTestResult(
                    label = "${llmManager.getVendor(entry.vendorId).name} / ${entry.model}",
                    success = r.isSuccess,
                    detail = r.exceptionOrNull()?.message ?: "连接成功"
                )
            }
            val ok = results.count { it.success }
            _uiState.update {
                it.copy(
                    testing = false,
                    testResults = results,
                    message = "连接测试完成：成功 $ok / ${results.size}"
                )
            }
        }
    }

    // ---------------- Prompt ----------------

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

    // ---------------- 备份 ----------------

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
                it.copy(
                    entries = apiKeyStore.getEntries(),
                    profile = profileRepository.getProfile(),
                    message = result.fold(
                        onSuccess = { "已恢复备份：$it" },
                        onFailure = { "导入失败：${it.message}" }
                    )
                )
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
