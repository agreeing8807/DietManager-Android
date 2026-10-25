package com.example.diet.ui.water

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.diet.data.local.entity.DrinkPreset
import com.example.diet.data.local.entity.WaterRecord
import com.example.diet.domain.DrinkCalculator
import com.example.diet.ui.component.Chip
import com.example.diet.ui.component.SectionCard
import com.example.diet.ui.navigation.Routes
import com.example.diet.util.DateUtils
import kotlin.math.roundToInt

private val DrinkBlue = Color(0xFF0288D1)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaterScreen(navController: NavHostController, viewModel: WaterViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedPresetId by remember { mutableStateOf(0L) }
    val preset = state.presets.firstOrNull { it.id == selectedPresetId }
        ?: state.presets.firstOrNull()
    var volume by remember { mutableStateOf(250f) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<WaterRecord?>(null) }

    val waterGoal = state.profile.waterGoalMl.coerceAtLeast(1)
    val progress = state.todayWaterMl.toFloat() / waterGoal
    val preview = preset?.let { DrinkCalculator.nutritionOf(it, volume.roundToInt()) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("喝水", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        DateUtils.dayLabel(state.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { navController.navigate(Routes.REMINDER) }) {
                    Icon(Icons.Filled.Schedule, contentDescription = "喝水提醒")
                }
            }
        }

        // ---------------- 今日进度 ----------------
        item {
            SectionCard {
                Text(
                    "今日饮水 ${state.todayWaterMl} / $waterGoal ml",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = DrinkBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (state.todayCalories > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "饮品额外摄入 ${state.todayCalories} kcal · 蛋白质 ${state.proteinG.roundToInt()} g · " +
                            "碳水 ${state.carbG.roundToInt()} g · 脂肪 ${state.fatG.roundToInt()} g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---------------- 水种 ----------------
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("水种", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showCustomDialog = true }) { Text("自定义") }
                }
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.presets.forEach { p ->
                        Chip(
                            text = p.name,
                            selected = p.id == (preset?.id ?: -1L),
                            onClick = { selectedPresetId = p.id }
                        )
                    }
                }
                if (preset != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "每 100ml：水分 ${(preset.waterRatio * 100).roundToInt()}% · " +
                            "热量 ${preset.caloriesPer100ml.roundToInt()} kcal · " +
                            "蛋白质 ${preset.proteinPer100ml} g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---------------- 摄入量 ----------------
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("摄入量", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${volume.roundToInt()} ml",
                        style = MaterialTheme.typography.titleMedium,
                        color = DrinkBlue
                    )
                }
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..800f,
                    steps = 79
                )
                Text(
                    "每格 10 ml，可左右滑动微调",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (preview != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "本次：水分 ${preview.waterMl} ml · 热量 ${preview.caloriesKcal} kcal · " +
                            "蛋白质 ${preview.proteinG.roundToInt()} g · " +
                            "碳水 ${preview.carbG.roundToInt()} g · 脂肪 ${preview.fatG.roundToInt()} g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        preset?.let { viewModel.addDrink(it, volume.roundToInt()) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = preset != null && volume.roundToInt() > 0
                ) {
                    Text("饮用 ${volume.roundToInt()} ml" + (preset?.let { " · ${it.name}" } ?: ""))
                }
            }
        }

        // ---------------- 今日时间线 ----------------
        item {
            Text("今日记录", style = MaterialTheme.typography.titleMedium)
        }
        if (state.todayRecords.isEmpty()) {
            item {
                Text(
                    "今天还没有饮水记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.todayRecords, key = { it.id }) { record ->
                WaterTimelineRow(
                    record = record,
                    onEdit = { editingRecord = record },
                    onDelete = { viewModel.deleteDrink(record) }
                )
            }
        }

        // ---------------- 往日汇总（只读） ----------------
        if (state.pastDays.isNotEmpty()) {
            item {
                Text("往日记录", style = MaterialTheme.typography.titleMedium)
            }
            items(state.pastDays, key = { it.date }) { day ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            DateUtils.dayLabel(day.date),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${day.volumeMl} ml",
                            style = MaterialTheme.typography.titleMedium,
                            color = DrinkBlue
                        )
                    }
                }
            }
            item {
                Text(
                    "往日仅保留每日总量，不可修改",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showCustomDialog) {
        CustomPresetDialog(
            customPresets = state.presets.filter { !it.builtIn },
            onDismiss = { showCustomDialog = false },
            onAdd = { name, ratio, kcal, protein, carb, fat ->
                viewModel.addCustomPreset(name, ratio, kcal, protein, carb, fat)
            },
            onDelete = { viewModel.deletePreset(it) }
        )
    }

    editingRecord?.let { record ->
        EditWaterDialog(
            record = record,
            presets = state.presets,
            onDismiss = { editingRecord = null },
            onSave = { p, ml ->
                viewModel.updateDrink(record, p, ml)
                editingRecord = null
            }
        )
    }
}

@Composable
private fun WaterTimelineRow(record: WaterRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(48.dp)) {
                Text(
                    DateUtils.formatTime(record.recordedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    (record.drinkName.ifBlank { record.type.label }) + "  ${record.volumeMl} ml",
                    style = MaterialTheme.typography.titleMedium
                )
                val details = buildList {
                    add("水分 ${if (record.waterMl > 0) record.waterMl else record.volumeMl} ml")
                    if (record.caloriesKcal > 0) add("${record.caloriesKcal} kcal")
                    if (record.proteinG > 0f) add("蛋白 ${record.proteinG.roundToInt()} g")
                }
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "修改", Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", Modifier.size(18.dp), tint = Color(0xFFE53935))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditWaterDialog(
    record: WaterRecord,
    presets: List<DrinkPreset>,
    onDismiss: () -> Unit,
    onSave: (DrinkPreset, Int) -> Unit
) {
    var presetId by remember { mutableStateOf(presets.firstOrNull { it.name == record.drinkName }?.id ?: 0L) }
    var volume by remember { mutableStateOf(record.volumeMl.toFloat()) }
    val preset = presets.firstOrNull { it.id == presetId } ?: presets.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改饮水记录") },
        text = {
            Column(
                Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { p ->
                        Chip(p.name, p.id == (preset?.id ?: -1L), onClick = { presetId = p.id })
                    }
                }
                Text("容量 ${volume.roundToInt()} ml", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..800f,
                    steps = 79
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { preset?.let { onSave(it, volume.roundToInt()) } ?: onDismiss() },
                enabled = preset != null && volume.roundToInt() > 0
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun CustomPresetDialog(
    customPresets: List<DrinkPreset>,
    onDismiss: () -> Unit,
    onAdd: (String, Int, Float, Float, Float, Float) -> Unit,
    onDelete: (DrinkPreset) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ratio by remember { mutableStateOf("100") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义水种") },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "按每 100 ml 填写比例，无照片时直接按比例计算",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                NumField("水分比例 (%)", ratio, decimal = false) { ratio = it }
                NumField("热量 (kcal/100ml)", kcal, decimal = true) { kcal = it }
                NumField("蛋白质 (g/100ml)", protein, decimal = true) { protein = it }
                NumField("碳水 (g/100ml)", carb, decimal = true) { carb = it }
                NumField("脂肪 (g/100ml)", fat, decimal = true) { fat = it }
                Button(
                    onClick = {
                        onAdd(
                            name,
                            ratio.toIntOrNull() ?: 100,
                            kcal.toFloatOrNull() ?: 0f,
                            protein.toFloatOrNull() ?: 0f,
                            carb.toFloatOrNull() ?: 0f,
                            fat.toFloatOrNull() ?: 0f
                        )
                        name = ""; ratio = "100"; kcal = ""; protein = ""; carb = ""; fat = ""
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("添加水种") }

                if (customPresets.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    customPresets.forEach { p ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "水分 ${(p.waterRatio * 100).roundToInt()}% · " +
                                        "${p.caloriesPer100ml.roundToInt()} kcal/100ml",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDelete(p) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "删除水种",
                                    Modifier.size(18.dp),
                                    tint = Color(0xFFE53935)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}

@Composable
private fun NumField(label: String, value: String, decimal: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() || (decimal && it == '.') }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
