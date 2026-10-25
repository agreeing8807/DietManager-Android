package com.example.diet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.dao.DailyCalories
import com.example.diet.data.local.dao.DailyVolume
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.local.entity.QuickItem
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.data.local.entity.WaterRecord
import com.example.diet.data.local.entity.WeightRecord
import com.example.diet.data.repository.MealRepository
import com.example.diet.data.repository.ProfileRepository
import com.example.diet.data.repository.QuickItemRepository
import com.example.diet.data.repository.WaterRepository
import com.example.diet.data.repository.WeightRepository
import com.example.diet.domain.model.DrinkType
import com.example.diet.domain.model.MealType
import com.example.diet.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.roundToInt

/** 某一天时间线上的一条记录（饮食或饮水） */
data class DayEvent(
    val timeLabel: String,
    val isDrink: Boolean,
    val title: String,
    val detail: String
)

data class DashboardUiState(
    val profile: UserProfile = UserProfile(),
    /** 今天（按自定义每日开始时间换算） */
    val date: String = "",
    /** 日历当前展示的月份 */
    val month: YearMonth = YearMonth.now(),
    /** 相对本月的偏移，0 = 本月，-1 = 上月 */
    val monthOffset: Int = 0,
    /** 日历中选中的日期 */
    val selectedDate: String = "",
    val calorieIntake: Int = 0,
    val waterMl: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    /** 近 30 天体重记录（每天取最后一条） */
    val weightTrend: List<WeightRecord> = emptyList(),
    val quickItems: List<QuickItem> = emptyList(),
    /** 当月每日饮水总量：yyyy-MM-dd -> ml */
    val monthWater: Map<String, Int> = emptyMap(),
    /** 当月每日热量总量：yyyy-MM-dd -> kcal */
    val monthCalories: Map<String, Int> = emptyMap(),
    /** 选中日期的完整时间线 */
    val selectedEvents: List<DayEvent> = emptyList(),
    val selectedWaterMl: Int = 0,
    val selectedCalories: Int = 0,
    val isLoading: Boolean = true
)

private data class TodayBundle(
    val meals: List<MealRecord>,
    val drinks: List<WaterRecord>,
    val weights: List<WeightRecord>,
    val quickItems: List<QuickItem>
)

private data class MonthBundle(
    val calories: List<DailyCalories>,
    val water: List<DailyVolume>
)

private data class SelectedBundle(
    val meals: List<MealRecord>,
    val drinks: List<WaterRecord>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository,
    private val quickItemRepository: QuickItemRepository
) : ViewModel() {

    private val monthOffset = MutableStateFlow(0)

    /** null 表示跟随“今天” */
    private val selectedDate = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = profileRepository.observeProfile()
        .flatMapLatest { profile ->
            val dayStart = profile.dayStartHour
            val today = DateUtils.today(dayStart)
            val currentMonth = YearMonth.from(DateUtils.effectiveDate(dayStart))
            val weightStart = DateUtils.dateOf(DateUtils.effectiveDate(dayStart).minusDays(29))

            combine(monthOffset, selectedDate) { offset, selected -> offset to (selected ?: today) }
                .flatMapLatest { (offset, selected) ->
                    val month = currentMonth.plusMonths(offset.toLong())
                    val monthStart = DateUtils.dateOf(month.atDay(1))
                    val monthEnd = DateUtils.dateOf(month.atEndOfMonth())

                    val todayBundle = combine(
                        mealRepository.observeByDate(today),
                        waterRepository.observeByDate(today),
                        weightRepository.observeBetween(weightStart, today),
                        quickItemRepository.observeAll()
                    ) { meals, drinks, weights, quickItems ->
                        TodayBundle(meals, drinks, weights, quickItems)
                    }

                    val monthBundle = combine(
                        mealRepository.observeDailyCaloriesBetween(monthStart, monthEnd),
                        waterRepository.observeDailyTotalsBetween(monthStart, monthEnd)
                    ) { calories, water -> MonthBundle(calories, water) }

                    val selectedBundle = combine(
                        mealRepository.observeByDate(selected),
                        waterRepository.observeByDate(selected)
                    ) { meals, drinks -> SelectedBundle(meals, drinks) }

                    combine(todayBundle, monthBundle, selectedBundle) { todayData, monthData, dayData ->
                        DashboardUiState(
                            profile = profile,
                            date = today,
                            month = month,
                            monthOffset = offset,
                            selectedDate = selected,
                            calorieIntake = todayData.meals.sumOf { it.caloriesKcal } +
                                todayData.drinks.sumOf { it.caloriesKcal },
                            waterMl = todayData.drinks.sumOf { if (it.waterMl > 0) it.waterMl else it.volumeMl },
                            proteinG = todayData.meals.fold(0f) { acc, m -> acc + m.proteinG } +
                                todayData.drinks.fold(0f) { acc, d -> acc + d.proteinG },
                            carbG = todayData.meals.fold(0f) { acc, m -> acc + m.carbG } +
                                todayData.drinks.fold(0f) { acc, d -> acc + d.carbG },
                            fatG = todayData.meals.fold(0f) { acc, m -> acc + m.fatG } +
                                todayData.drinks.fold(0f) { acc, d -> acc + d.fatG },
                            weightTrend = todayData.weights
                                .groupBy { it.date }
                                .map { (_, sameDay) ->
                                    sameDay.maxByOrNull { r -> r.recordedAt } ?: sameDay.first()
                                }
                                .sortedBy { it.date },
                            quickItems = todayData.quickItems,
                            monthWater = monthData.water.associate { it.date to it.volumeMl },
                            // 热量 = 饮食 + 饮品，与“今日摄入进度”口径一致
                            monthCalories = mergeCalories(monthData.calories, monthData.water),
                            selectedEvents = buildEvents(dayData.meals, dayData.drinks),
                            selectedWaterMl = dayData.drinks.sumOf {
                                if (it.waterMl > 0) it.waterMl else it.volumeMl
                            },
                            selectedCalories = dayData.meals.sumOf { it.caloriesKcal } +
                                dayData.drinks.sumOf { it.caloriesKcal },
                            isLoading = false
                        )
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    /** 日历翻月：0 为本月，最多回溯 24 个月，不允许翻到未来 */
    fun shiftMonth(delta: Int) {
        val next = (monthOffset.value + delta).coerceIn(-24, 0)
        if (next == monthOffset.value) return
        monthOffset.value = next

        // 选中日期跟随翻月：回到本月则选今天，其它月落到同一天（越界取月末）
        val today = uiState.value.date
        val todayDate = DateUtils.parse(today) ?: return
        if (next == 0) {
            selectedDate.value = null
            return
        }
        val targetMonth = YearMonth.from(todayDate).plusMonths(next.toLong())
        val dayOfMonth = DateUtils.parse(uiState.value.selectedDate)?.dayOfMonth ?: 1
        selectedDate.value = DateUtils.dateOf(
            targetMonth.atDay(dayOfMonth.coerceAtMost(targetMonth.lengthOfMonth()))
        )
    }

    fun resetMonth() {
        monthOffset.value = 0
        selectedDate.value = null
    }

    fun selectDate(date: String) {
        val today = uiState.value.date
        if (today.isNotBlank() && date > today) return
        selectedDate.value = date
    }

    /** 记录体重，并同步资料里的当前体重 */
    fun logWeight(weightKg: Float) {
        if (weightKg <= 0f || weightKg > 400f) return
        viewModelScope.launch {
            val date = DateUtils.today(uiState.value.profile.dayStartHour)
            weightRepository.addWeight(WeightRecord(date = date, weightKg = weightKg))
            profileRepository.updateCurrentWeight(weightKg)
        }
    }

    /**
     * 一键记录常用饮食：热量与营养计入当天饮食，水分计入当天饮水。
     * 水分记录热量置 0，避免与饮食记录重复计数。
     */
    fun applyQuickItem(item: QuickItem) {
        viewModelScope.launch {
            val profile = uiState.value.profile
            val date = DateUtils.today(profile.dayStartHour)
            val hasNutrition = item.caloriesKcal > 0 || item.proteinG > 0f ||
                item.carbG > 0f || item.fatG > 0f
            if (hasNutrition) {
                mealRepository.addMeal(
                    MealRecord(
                        date = date,
                        type = MealType.fromHour(LocalTime.now().hour),
                        description = item.name,
                        caloriesKcal = item.caloriesKcal,
                        proteinG = item.proteinG,
                        carbG = item.carbG,
                        fatG = item.fatG,
                        summary = "快捷记录",
                        analyzedByAi = false
                    )
                )
            }
            if (item.waterMl > 0) {
                waterRepository.addWater(
                    WaterRecord(
                        date = date,
                        type = DrinkType.OTHER,
                        volumeMl = item.waterMl,
                        waterMl = item.waterMl,
                        drinkName = item.name,
                        caloriesKcal = 0,
                        isMilkTea = false
                    )
                )
            }
        }
    }

    fun saveQuickItem(item: QuickItem) {
        viewModelScope.launch {
            if (item.id == 0L) quickItemRepository.add(item) else quickItemRepository.update(item)
        }
    }

    fun deleteQuickItem(item: QuickItem) {
        viewModelScope.launch { quickItemRepository.delete(item) }
    }

    /** 按天合并“饮食热量 + 饮品热量” */
    private fun mergeCalories(
        meals: List<DailyCalories>,
        drinks: List<DailyVolume>
    ): Map<String, Int> {
        val map = HashMap<String, Int>()
        meals.forEach { day -> map[day.date] = (map[day.date] ?: 0) + day.caloriesKcal }
        drinks.forEach { day -> map[day.date] = (map[day.date] ?: 0) + day.caloriesKcal }
        return map
    }

    /** 合并饮食与饮水记录，按时间升序 */
    private fun buildEvents(meals: List<MealRecord>, drinks: List<WaterRecord>): List<DayEvent> {
        val events = ArrayList<Pair<Long, DayEvent>>(meals.size + drinks.size)
        meals.forEach { meal ->
            events += meal.createdAt to DayEvent(
                timeLabel = DateUtils.formatTime(meal.createdAt),
                isDrink = false,
                title = meal.description,
                detail = buildString {
                    append(meal.type.label)
                    append(" · ${meal.caloriesKcal} kcal")
                    if (meal.proteinG > 0f) append(" · 蛋白 ${meal.proteinG.roundToInt()} g")
                    if (meal.carbG > 0f) append(" · 碳水 ${meal.carbG.roundToInt()} g")
                    if (meal.fatG > 0f) append(" · 脂肪 ${meal.fatG.roundToInt()} g")
                }
            )
        }
        drinks.forEach { drink ->
            events += drink.recordedAt to DayEvent(
                timeLabel = DateUtils.formatTime(drink.recordedAt),
                isDrink = true,
                title = drink.drinkName.ifBlank { drink.type.label },
                detail = buildString {
                    if (drink.servings > 0f) {
                        // 冲调型：份数决定营养，水量决定总容量
                        append("×${servingsLabel(drink.servings)} 份")
                        append(" · 加水 ${drink.waterMl} ml")
                        append(" · 总 ${drink.volumeMl} ml")
                    } else {
                        append("${drink.volumeMl} ml")
                        append(" · 水分 ${if (drink.waterMl > 0) drink.waterMl else drink.volumeMl} ml")
                    }
                    if (drink.caloriesKcal > 0) append(" · ${drink.caloriesKcal} kcal")
                }
            )
        }
        return events.sortedBy { it.first }.map { it.second }
    }

    /** 1 份显示为「1」，1.5 份显示为「1.5」 */
    private fun servingsLabel(value: Float): String =
        if (value % 1f == 0f) value.roundToInt().toString()
        else String.format(java.util.Locale.CHINA, "%.1f", value)
}
