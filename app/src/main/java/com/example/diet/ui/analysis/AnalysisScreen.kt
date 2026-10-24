package com.example.diet.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diet.domain.NutritionCalculator
import com.example.diet.ui.component.MacroBar
import com.example.diet.ui.component.SectionCard
import com.example.diet.ui.component.WaterBarChart
import com.example.diet.ui.component.WeightLineChart
import com.example.diet.ui.component.oneDecimal
import kotlin.math.abs
import kotlin.math.roundToInt

private val ProteinColor = Color(0xFF42A5F5)
private val CarbColor = Color(0xFFFFB74D)
private val FatColor = Color(0xFFAB47BC)

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val suggestionLoading by viewModel.suggestionLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val goal = state.profile?.dailyCalorieGoal ?: 1800
    val waterGoal = state.profile?.waterGoalMl ?: 2000
    val currentWeight = state.profile?.currentWeightKg ?: 65f
    val targetWeight = state.profile?.targetWeightKg ?: currentWeight
    val remaining = (goal - state.calorieIntake).coerceAtLeast(0)
    val over = state.calorieIntake > goal
    val statusColor = if (over) Color(0xFFE53935) else Color(0xFF43A047)
    val statusText = if (over) "已超出 ${state.calorieIntake - goal} kcal" else "剩余 ${remaining} kcal"

    val targets = remember(goal, currentWeight) { NutritionCalculator.macroTargets(goal, currentWeight) }
    val weightPoints = remember(state.weightTrend) { state.weightTrend.map { it.date to it.weightKg } }
    val latestWeight = weightPoints.lastOrNull()?.second ?: currentWeight
    val firstWeight = weightPoints.firstOrNull()?.second ?: currentWeight
    val weightChange = latestWeight - firstWeight
    val reachedDays = state.waterReachedDays(waterGoal)

    var showWeightDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("分析与建议", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            SectionCard {
                Text("今日热量对比", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("摄入 ${state.calorieIntake} kcal", style = MaterialTheme.typography.bodyLarge)
                    Text("目标 $goal kcal", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(6.dp))
                Text(statusText, color = statusColor, style = MaterialTheme.typography.titleMedium)
            }
        }

        item {
            SectionCard {
                Text("每日营养素目标", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "按当前体重 ${oneDecimal(currentWeight)} kg、热量目标 $goal kcal 推算",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                MacroBar("蛋白质", state.proteinIntake, targets.proteinG, ProteinColor)
                MacroBar("碳水", state.carbIntake, targets.carbG, CarbColor)
                MacroBar("脂肪", state.fatIntake, targets.fatG, FatColor)
                Spacer(Modifier.height(8.dp))
                Text(
                    "目标：蛋白质 ${targets.proteinG.roundToInt()} g（${(targets.proteinRatio * 100).roundToInt()}%）· " +
                        "碳水 ${targets.carbG.roundToInt()} g（${(targets.carbRatio * 100).roundToInt()}%）· " +
                        "脂肪 ${targets.fatG.roundToInt()} g（${(targets.fatRatio * 100).roundToInt()}%）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "蛋白质 1.6 g/kg 体重 · 脂肪占总热量 25% · 碳水补足剩余热量",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("体重趋势", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showWeightDialog = true }) { Text("记录体重") }
                }

                if (weightPoints.isEmpty()) {
                    Text(
                        "近 30 天还没有体重记录，记录一次即可生成曲线",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatItem("当前", "${oneDecimal(latestWeight)} kg", Modifier.weight(1f))
                        StatItem(
                            label = "30 天变化",
                            value = when {
                                abs(weightChange) < 0.05f -> "持平"
                                weightChange > 0 -> "+${oneDecimal(weightChange)} kg"
                                else -> "${oneDecimal(weightChange)} kg"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StatItem("目标", "${oneDecimal(targetWeight)} kg", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    WeightLineChart(
                        points = weightPoints,
                        targetKg = targetWeight
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        weightInsight(latestWeight, targetWeight, weightChange),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("饮水分析", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Text(
                        "近 7 天",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatItem("日均", "${state.waterAverage} ml", Modifier.weight(1f), Color(0xFF0288D1))
                    StatItem("达标", "$reachedDays / 7 天", Modifier.weight(1f), Color(0xFF0288D1))
                    StatItem("今日", "${state.waterWeek.lastOrNull()?.volumeMl ?: 0} ml", Modifier.weight(1f), Color(0xFF0288D1))
                }
                Spacer(Modifier.height(10.dp))
                WaterBarChart(
                    daily = state.waterWeek.map { it.date to it.volumeMl },
                    goalMl = waterGoal
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    waterInsight(state.waterAverage, waterGoal, reachedDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SectionCard {
                Text("AI 餐食分析", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (state.meals.isEmpty()) {
                    Text("今天还没有记录三餐", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.meals.forEach { meal ->
                        if (meal.summary.isNotBlank()) {
                            Text("· ${meal.type.label}：${meal.summary}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("推荐食物", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = { viewModel.loadSuggestions() }, enabled = !suggestionLoading) {
                    if (suggestionLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("获取建议")
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }

        items(suggestions, key = { it.name }) { s ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium)
                        Text("${s.caloriesKcal} kcal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(s.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showWeightDialog) {
        var input by remember { mutableStateOf(oneDecimal(latestWeight)) }
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("记录今日体重") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { text ->
                        input = text.filter { it.isDigit() || it == '.' }.take(5)
                    },
                    label = { Text("体重 (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    input.toFloatOrNull()?.let { viewModel.logWeight(it) }
                    showWeightDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun weightInsight(current: Float, target: Float, change: Float): String {
    val gap = current - target
    val trend = when {
        abs(change) < 0.05f -> "近期体重基本持平"
        change < 0 -> "近 30 天已下降 ${oneDecimal(-change)} kg"
        else -> "近 30 天上升了 ${oneDecimal(change)} kg"
    }
    return when {
        abs(gap) < 0.5f -> "$trend，已接近目标体重，注意保持。"
        gap > 0 -> "$trend，距目标还需减 ${oneDecimal(gap)} kg，建议保持热量缺口约 500 kcal/天。"
        else -> "$trend，距目标还需增 ${oneDecimal(-gap)} kg，建议适度增加蛋白质与力量训练。"
    }
}

private fun waterInsight(average: Int, goalMl: Int, reachedDays: Int): String {
    if (goalMl <= 0) return ""
    return when {
        reachedDays >= 6 -> "近 7 天有 $reachedDays 天达标，饮水习惯保持得很好。"
        average >= goalMl -> "日均 ${average} ml 已达标，继续保持规律饮水。"
        else -> "日均 ${average} ml，距目标还差 ${goalMl - average} ml，可每隔 1-2 小时补水 200-300 ml。"
    }
}
