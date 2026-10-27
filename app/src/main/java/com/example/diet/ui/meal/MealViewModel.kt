package com.example.diet.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.entity.MealItem
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.data.repository.AnalysisRepository
import com.example.diet.data.repository.MealRepository
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.data.repository.WaterRepository
import com.example.diet.domain.DailyIntake
import com.example.diet.domain.DayLedger
import com.example.diet.domain.MealItems
import com.example.diet.domain.model.DataStatus
import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.domain.model.MealAnalysis
import com.example.diet.domain.model.MealType
import com.example.diet.domain.model.NutritionLabel
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

/** 当前展示的日期与该日用餐记录 */
data class MealDayState(
    val date: String = "",
    val today: String = "",
    val meals: List<MealRecord> = emptyList(),
    val profile: UserProfile = UserProfile(),
    /** 当日摄入统一口径（含饮品），由 DayLedger 计算 */
    val intake: DailyIntake = DailyIntake.EMPTY
) {
    val totalCalories: Int get() = intake.caloriesKcal
    val proteinG: Float get() = intake.proteinG
    val carbG: Float get() = intake.carbG
    val fatG: Float get() = intake.fatG
    val waterMl: Int get() = intake.waterMl
    /** 营养数据不完整的餐食条数 */
    val pendingMeals: Int get() = intake.pendingMeals
    val isToday: Boolean get() = date == today
}

data class MealUiState(
    val analyzing: Boolean = false,
    val message: String? = null,
    val suggestions: List<FoodSuggestion> = emptyList(),
    val suggesting: Boolean = false,
    val error: String? = null,
    /** 营养成分表识别结果；非空时界面弹出确认框 */
    val label: NutritionLabel? = null,
    val labelLoading: Boolean = false,
    val labelError: String? = null,
    /** 按第二张照片估出的克数，用于回填输入框 */
    val estimatedGrams: Float? = null,
    val estimatingPortion: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val profileRepository: ProfileRepository,
    private val waterRepository: WaterRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    /** 空串表示跟随“今天” */
    private val selectedDate = MutableStateFlow("")

    val dayState: StateFlow<MealDayState> =
        combine(profileRepository.observeProfile(), selectedDate) { profile, selected ->
            val today = DateUtils.today(profile.dayStartHour)
            Triple(profile, today, selected.ifBlank { today })
        }.flatMapLatest { (profile, today, date) ->
            // 饮品与餐食一起纳入同一口径，避免各页面各算一套
            combine(
                mealRepository.observeByDate(date),
                waterRepository.observeByDate(date)
            ) { meals, drinks ->
                MealDayState(
                    date = date,
                    today = today,
                    meals = meals.sortedBy { it.createdAt },
                    profile = profile,
                    intake = DayLedger.of(meals, drinks)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MealDayState())

    private val _uiState = MutableStateFlow(MealUiState())
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    /** 正在查看明细的记录 id，0 表示未打开 */
    private val detailRecordId = MutableStateFlow(0L)

    val detailItems: StateFlow<List<MealItem>> = detailRecordId
        .flatMapLatest { id ->
            if (id == 0L) flowOf(emptyList()) else mealRepository.observeItems(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                analyzedByAi = analysis != null,
                // 分析失败时数值全为 0，明确标记为缺失而非"估算"
                status = analysis?.status ?: DataStatus.MISSING
            )
            val recordId = mealRepository.addMeal(record)
            // 明细落库：只存每 100g 基准与克数，记录头的总量由明细重算
            mealRepository.addItems(
                recordId = recordId,
                items = analysis?.items.orEmpty()
                    .mapIndexed { index, item -> MealItems.toEntity(recordId, index, item) }
            )
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

    // ---------------- 营养成分表 ----------------

    /**
     * 拍照读取包装营养成分表。
     * 模型只负责 OCR，这里拿到的是标签原始数值，克数由用户决定后才落库。
     */
    fun readNutritionLabel(imageBytes: ByteArray) {
        if (_uiState.value.labelLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(labelLoading = true, labelError = null) }
            analysisRepository.readNutritionLabel(imageBytes).fold(
                onSuccess = { label ->
                    val usable = label.isUsable
                    _uiState.update {
                        it.copy(
                            labelLoading = false,
                            label = label.takeIf { usable },
                            labelError = if (usable) null else "没读到有效数值，请对准营养成分表、保证文字清晰后重拍"
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(labelLoading = false, labelError = e.message ?: "读取营养成分表失败")
                    }
                }
            )
        }
    }

    /** 用第二张照片估算克重，只回填确认框，不落库 */
    fun estimatePortion(imageBytes: ByteArray) {
        val label = _uiState.value.label ?: return
        if (_uiState.value.estimatingPortion) return
        viewModelScope.launch {
            _uiState.update { it.copy(estimatingPortion = true) }
            analysisRepository.estimateGrams(imageBytes, label.name).fold(
                onSuccess = { grams ->
                    _uiState.update {
                        it.copy(
                            estimatingPortion = false,
                            estimatedGrams = grams.takeIf { g -> g > 0f },
                            message = if (grams > 0f) {
                                "已按照片估算 ${grams.roundToInt()} g"
                            } else {
                                "未能估算份量，请手动填写克数"
                            }
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(estimatingPortion = false, message = "份量估算失败：${e.message}")
                    }
                }
            )
        }
    }

    fun dismissLabel() {
        _uiState.update {
            it.copy(label = null, labelError = null, estimatedGrams = null, estimatingPortion = false)
        }
    }

    /**
     * 用营养成分表 + 克数直接落库：每 100g 数据已由标签换算，热量本地计算，不再调用模型。
     * @return 是否受理
     */
    fun addFromLabel(
        type: MealType,
        name: String,
        brand: String,
        kcalPer100: Float,
        proteinPer100: Float,
        carbPer100: Float,
        fatPer100: Float,
        grams: Float
    ): Boolean {
        if (name.isBlank() || grams <= 0f) return false
        viewModelScope.launch {
            val date = DateUtils.today(dayState.value.profile.dayStartHour)
            val item = MealItem(
                recordId = 0L,
                name = name.trim(),
                brand = brand.trim(),
                grams = grams.coerceIn(0f, 5000f),
                kcalPer100g = kcalPer100.coerceAtLeast(0f),
                proteinPer100g = proteinPer100.coerceAtLeast(0f),
                carbPer100g = carbPer100.coerceAtLeast(0f),
                fatPer100g = fatPer100.coerceAtLeast(0f),
                source = "包装营养标签",
                confidence = 1f,
                status = DataStatus.CONFIRMED
            )
            val nutrition = MealItems.nutritionOf(item)
            val record = MealRecord(
                date = date,
                type = type,
                description = if (item.brand.isBlank()) item.name else "${item.brand} ${item.name}",
                caloriesKcal = nutrition.kcal,
                proteinG = nutrition.proteinG,
                carbG = nutrition.carbG,
                fatG = nutrition.fatG,
                summary = "按包装营养成分表：每 100g ${item.kcalPer100g.roundToInt()} kcal，" +
                    "本次 ${item.grams.roundToInt()}g 共 ${nutrition.kcal} kcal",
                analyzedByAi = true,
                status = DataStatus.CONFIRMED
            )
            val recordId = mealRepository.addMeal(record)
            mealRepository.addItems(recordId, listOf(item.copy(recordId = recordId)))
            mealRepository.syncTotals(recordId)
            _uiState.update {
                it.copy(
                    label = null,
                    labelError = null,
                    estimatedGrams = null,
                    message = "已按营养成分表添加"
                )
            }
        }
        return true
    }

    fun updateMeal(record: MealRecord, type: MealType, description: String, calories: Int) {
        viewModelScope.launch {
            val trimmed = description.ifBlank { record.description }
            if (mealRepository.getItems(record.id).isEmpty()) {
                // 没有明细（历史记录 / 手动记录）：记录头就是唯一数据源
                mealRepository.updateMeal(
                    record.copy(
                        type = type,
                        description = trimmed,
                        caloriesKcal = calories.coerceAtLeast(0),
                        status = when {
                            calories > 0 -> DataStatus.CONFIRMED
                            else -> record.status
                        }
                    )
                )
            } else {
                // 有明细：数值以明细为准，避免记录头与条目两处数字打架
                mealRepository.updateMeal(record.copy(type = type, description = trimmed))
                mealRepository.syncTotals(record.id)
            }
        }
    }

    fun deleteMeal(record: MealRecord) {
        viewModelScope.launch { mealRepository.deleteMeal(record) }
    }

    // ---------------- 条目明细 ----------------

    fun openDetail(record: MealRecord) {
        detailRecordId.value = record.id
    }

    fun closeDetail() {
        detailRecordId.value = 0L
    }

    /**
     * 修正某项克数：营养按每 100g 基准本地重算，并标记该项已由用户确认。
     * 不需要重新调用模型，也不会改动其他条目。
     */
    fun updateItemGrams(item: MealItem, grams: Float) {
        if (grams <= 0f) return
        viewModelScope.launch {
            mealRepository.updateItem(
                item.copy(
                    grams = grams.coerceIn(0f, 5000f),
                    status = DataStatus.CONFIRMED
                )
            )
        }
    }

    /**
     * 设置某项的水分（ml）：奶茶、果汁这类饮品在餐食里也能计入饮水统计。
     * 填 0 表示该条目不含水分；不改变营养与克数。
     */
    fun updateItemWater(item: MealItem, waterMl: Float) {
        viewModelScope.launch {
            mealRepository.updateItem(
                item.copy(waterMl = waterMl.coerceIn(0f, 5000f))
            )
        }
    }

    /** 删除某项后，记录头会自动重算；删空则整餐标记为缺失 */
    fun deleteItem(item: MealItem) {
        viewModelScope.launch { mealRepository.deleteItem(item) }
    }

    /**
     * 让失败可逆：
     * - 没有明细（分析失败 / 历史记录）→ 用原始描述整餐重新识别；
     * - 有明细但缺营养 → 只补缺失项，不动已确认的名称与克数。
     */
    fun reanalyze(record: MealRecord) {
        if (_uiState.value.analyzing) return
        viewModelScope.launch {
            _uiState.update { it.copy(analyzing = true, message = null) }
            val items = mealRepository.getItems(record.id)
            val message = if (items.isEmpty()) {
                analysisRepository.analyzeMeal(record.description, null).fold(
                    onSuccess = { analysis ->
                        mealRepository.replaceItems(
                            recordId = record.id,
                            items = analysis.items.mapIndexed { index, item ->
                                MealItems.toEntity(record.id, index, item)
                            }
                        )
                        // 只写摘要与状态：汇总字段已由 replaceItems 内的 syncTotals 重算
                        mealRepository.updateSummary(
                            recordId = record.id,
                            summary = analysis.summary,
                            status = analysis.status
                        )
                        if (analysis.items.isEmpty()) {
                            "仍未识别出食物，可补充描述后重试"
                        } else {
                            "已重新识别"
                        }
                    },
                    onFailure = { "未完成：${it.message ?: "重新识别失败"}" }
                )
            } else {
                val missing = items.filter { !it.hasBasis }
                if (missing.isEmpty()) {
                    "该餐条目数据完整，无需补全"
                } else {
                    analysisRepository.fillNutrition(missing.map(MealItems::toAnalysisItem)).fold(
                        onSuccess = { filled ->
                            val updates = filled.mapIndexedNotNull { index, item ->
                                val origin = missing.getOrNull(index) ?: return@mapIndexedNotNull null
                                if (item.per100 == null) return@mapIndexedNotNull null
                                MealItems.toEntity(record.id, index, item)
                                    // 保留用户手动登记的水分，补数只改营养基准
                                    .copy(
                                        id = origin.id,
                                        sortOrder = origin.sortOrder,
                                        waterMl = origin.waterMl
                                    )
                            }
                            if (updates.isEmpty()) {
                                "仍未查到可靠数据，可手动补克数或删除该条目"
                            } else {
                                mealRepository.updateItems(updates)
                                "已补全 ${updates.size} 项营养数据"
                            }
                        },
                        onFailure = { "未完成：${it.message ?: "补全失败"}" }
                    )
                }
            }
            _uiState.update { it.copy(analyzing = false, message = message) }
        }
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
