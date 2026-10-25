package com.example.diet.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.diet.data.local.entity.QuickItem
import com.example.diet.domain.NutritionCalculator
import com.example.diet.ui.component.MacroBar
import com.example.diet.ui.component.RingCalendar
import com.example.diet.ui.component.SectionCard
import com.example.diet.ui.component.WeightLineChart
import com.example.diet.ui.component.oneDecimal
import com.example.diet.util.DateUtils
import kotlin.math.abs
import kotlin.math.roundToInt

private val WaterColor = Color(0xFF0288D1)
private val CalorieColor = Color(0xFFFF7043)
private val ProteinColor = Color(0xFF42A5F5)
private val CarbColor = Color(0xFFFFB74D)
private val FatColor = Color(0xFFAB47BC)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile

    val calorieGoal = profile.dailyCalorieGoal.coerceAtLeast(1)
    val waterGoal = profile.waterGoalMl.coerceAtLeast(1)
    val targets = remember(calorieGoal, profile.currentWeightKg) {
        NutritionCalculator.macroTargets(calorieGoal, profile.currentWeightKg)
    }

    val weightPoints = state.weightTrend.map { it.date to it.weightKg }
    val latestWeight = weightPoints.lastOrNull()?.second ?: profile.currentWeightKg
    val firstWeight = weightPoints.firstOrNull()?.second ?: profile.currentWeightKg
    val weightChange = latestWeight - firstWeight

    var showWeightDialog by remember { mutableStateOf(false) }
    var showQuickManager by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<QuickItem?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("今日概览", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        DateUtils.dayLabel(state.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "目标 ${oneDecimal(profile.targetWeightKg)} kg",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ---------------- 体重曲线 ----------------
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("体重变化", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showWeightDialog = true }) { Text("记录") }
                }
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
                    StatItem("目标", "${oneDecimal(profile.targetWeightKg)} kg", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                if (weightPoints.isEmpty()) {
                    Text(
                        "还没有体重记录，点“记录”添加第一次称重即可生成曲线",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    WeightLineChart(points = weightPoints, targetKg = profile.targetWeightKg)
                }
            }
        }

        // ---------------- 摄入进度 ----------------
        item {
            SectionCard {
                Text("今日摄入进度", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                MacroBar("水分", state.waterMl.toFloat(), waterGoal.toFloat(), WaterColor, unit = "ml")
                MacroBar("热量", state.calorieIntake.toFloat(), calorieGoal.toFloat(), CalorieColor, unit = "kcal")
                MacroBar("蛋白质", state.proteinG, targets.proteinG, ProteinColor)
                MacroBar("碳水", state.carbG, targets.carbG, CarbColor)
                MacroBar("脂肪", state.fatG, targets.fatG, FatColor)
                if (state.pendingMeals > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${state.pendingMeals} 条餐食缺少营养数据，以上合计偏低于实际",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ---------------- 快捷吃喝 ----------------
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("快捷吃喝", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "点一下即可记录，右上角可编辑",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showQuickManager = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑快捷项")
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (state.quickItems.isEmpty()) {
                    Text(
                        "还没有快捷项，点右上角编辑按钮添加常用的食物或饮品",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.quickItems.forEach { item ->
                            FilledTonalButton(onClick = { viewModel.applyQuickItem(item) }) {
                                Text(item.name)
                            }
                        }
                    }
                }
            }
        }

        // ---------------- 饮食日历 + 当日时间线 ----------------
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("饮食日历", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (state.monthOffset != 0) {
                        TextButton(onClick = { viewModel.resetMonth() }) { Text("回到本月") }
                    }
                }
                RingCalendar(
                    month = state.month,
                    waterByDate = state.monthWater,
                    caloriesByDate = state.monthCalories,
                    waterGoal = waterGoal,
                    calorieGoal = calorieGoal,
                    selectedDate = state.selectedDate,
                    today = state.date,
                    onPrevMonth = { viewModel.shiftMonth(-1) },
                    onNextMonth = { viewModel.shiftMonth(1) },
                    canGoNext = state.monthOffset < 0,
                    onSelectDate = { viewModel.selectDate(it) }
                )

                HorizontalDivider(Modifier.padding(vertical = 10.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${DateUtils.dayLabel(state.selectedDate)} 记录",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${state.selectedCalories} kcal · ${state.selectedWaterMl} ml",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (state.selectedEvents.isEmpty()) {
                    Text(
                        "这一天没有饮食或饮水记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.selectedEvents.forEach { event ->
                        DayEventRow(event)
                    }
                }
            }
        }
    }

    if (showWeightDialog) {
        WeightDialog(
            initial = latestWeight,
            onDismiss = { showWeightDialog = false },
            onSave = { kg ->
                viewModel.logWeight(kg)
                showWeightDialog = false
            }
        )
    }

    if (showQuickManager) {
        QuickManagerDialog(
            items = state.quickItems,
            onDismiss = { showQuickManager = false },
            onEdit = { editingItem = it },
            onDelete = { viewModel.deleteQuickItem(it) },
            onAdd = { editingItem = QuickItem(name = "") }
        )
    }

    editingItem?.let { item ->
        QuickItemFormDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = {
                viewModel.saveQuickItem(it)
                editingItem = null
            }
        )
    }
}

@Composable
private fun DayEventRow(event: DayEvent) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            event.timeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(42.dp).padding(top = 2.dp)
        )
        Icon(
            if (event.isDrink) Icons.Filled.LocalDrink else Icons.Filled.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (event.isDrink) WaterColor else CalorieColor
        )
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                event.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeightDialog(initial: Float, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var input by remember { mutableStateOf(oneDecimal(initial)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录体重") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { text -> input = text.filter { it.isDigit() || it == '.' }.take(5) },
                label = { Text("体重 (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(onClick = { input.toFloatOrNull()?.let(onSave) ?: onDismiss() }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun QuickManagerDialog(
    items: List<QuickItem>,
    onDismiss: () -> Unit,
    onEdit: (QuickItem) -> Unit,
    onDelete: (QuickItem) -> Unit,
    onAdd: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑快捷吃喝") },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                if (items.isEmpty()) {
                    Text("暂无快捷项", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items.forEach { item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                quickItemSummary(item),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onEdit(item) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "编辑", Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDelete(item) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除",
                                Modifier.size(18.dp),
                                tint = Color(0xFFE53935)
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onAdd) { Text("新增项目") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}

@Composable
private fun QuickItemFormDialog(item: QuickItem, onDismiss: () -> Unit, onSave: (QuickItem) -> Unit) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var calories by remember(item.id) { mutableStateOf(item.caloriesKcal.takeIf { it > 0 }?.toString() ?: "") }
    var water by remember(item.id) { mutableStateOf(item.waterMl.takeIf { it > 0 }?.toString() ?: "") }
    var protein by remember(item.id) { mutableStateOf(item.proteinG.takeIf { it > 0f }?.let { oneDecimal(it) } ?: "") }
    var carb by remember(item.id) { mutableStateOf(item.carbG.takeIf { it > 0f }?.let { oneDecimal(it) } ?: "") }
    var fat by remember(item.id) { mutableStateOf(item.fatG.takeIf { it > 0f }?.let { oneDecimal(it) } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.id == 0L) "新增快捷项" else "编辑快捷项") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称（按钮上显示）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                NumField("热量 (kcal)", calories, false) { calories = it }
                NumField("水分 (ml)", water, false) { water = it }
                NumField("蛋白质 (g)", protein, true) { protein = it }
                NumField("碳水 (g)", carb, true) { carb = it }
                NumField("脂肪 (g)", fat, true) { fat = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            item.copy(
                                name = name.trim(),
                                caloriesKcal = calories.toIntOrNull() ?: 0,
                                waterMl = water.toIntOrNull() ?: 0,
                                proteinG = protein.toFloatOrNull() ?: 0f,
                                carbG = carb.toFloatOrNull() ?: 0f,
                                fatG = fat.toFloatOrNull() ?: 0f
                            )
                        )
                    }
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun NumField(label: String, value: String, decimal: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            onValueChange(text.filter { it.isDigit() || (decimal && it == '.') })
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun quickItemSummary(item: QuickItem): String {
    val parts = mutableListOf<String>()
    if (item.caloriesKcal > 0) parts += "${item.caloriesKcal} kcal"
    if (item.waterMl > 0) parts += "水 ${item.waterMl} ml"
    if (item.proteinG > 0f) parts += "蛋白 ${item.proteinG.roundToInt()} g"
    if (item.carbG > 0f) parts += "碳水 ${item.carbG.roundToInt()} g"
    if (item.fatG > 0f) parts += "脂肪 ${item.fatG.roundToInt()} g"
    return if (parts.isEmpty()) "未填写营养数据" else parts.joinToString(" · ")
}
