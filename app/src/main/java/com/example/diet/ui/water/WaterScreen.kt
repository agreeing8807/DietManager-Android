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
import com.example.diet.domain.DrinkNutrition
import com.example.diet.domain.PowderNutrition
import com.example.diet.domain.model.DrinkKind
import com.example.diet.ui.component.Chip
import com.example.diet.ui.component.SectionCard
import com.example.diet.ui.navigation.Routes
import com.example.diet.util.DateUtils
import java.util.Locale
import kotlin.math.roundToInt

private val DrinkBlue = Color(0xFF0288D1)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaterScreen(navController: NavHostController, viewModel: WaterViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedPresetId by remember { mutableStateOf(0L) }
    val preset = state.presets.firstOrNull { it.id == selectedPresetId }
        ?: state.presets.firstOrNull()
    val isPowder = preset?.kind == DrinkKind.POWDER
    var volume by remember { mutableStateOf(250f) }
    var servings by remember { mutableStateOf(1f) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<WaterRecord?>(null) }

    val waterGoal = state.profile.waterGoalMl.coerceAtLeast(1)
    val progress = state.todayWaterMl.toFloat() / waterGoal
    val liquidPreview = preset
        ?.takeIf { it.kind == DrinkKind.LIQUID }
        ?.let { DrinkCalculator.nutritionOf(it, volume.roundToInt()) }
    val powderPreview = preset
        ?.takeIf { it.kind == DrinkKind.POWDER }
        ?.let { DrinkCalculator.mixOf(it, servings, volume.roundToInt()) }

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
                            onClick = {
                                selectedPresetId = p.id
                                // 冲调型最少要加水，避免沿用液体型拖到很低的值
                                if (p.kind == DrinkKind.POWDER && volume < 50f) volume = 300f
                            }
                        )
                    }
                }
                if (preset != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        presetDetail(preset),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---------------- 摄入量 ----------------
        item {
            SectionCard {
                if (isPowder && preset != null) {
                    PowderIntakeSection(
                        preset = preset,
                        servings = servings,
                        waterMl = volume.roundToInt(),
                        preview = powderPreview,
                        onServingsChange = { servings = it },
                        onWaterChange = { volume = it },
                        onConfirm = { viewModel.addMixedDrink(preset, servings, volume.roundToInt()) }
                    )
                } else {
                    LiquidIntakeSection(
                        volume = volume,
                        preview = liquidPreview,
                        preset = preset,
                        onVolumeChange = { volume = it },
                        onConfirm = { preset?.let { viewModel.addDrink(it, volume.roundToInt()) } }
                    )
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
            onAddLiquid = { name, ratio, kcal, protein, carb, fat ->
                viewModel.addCustomPreset(name, ratio, kcal, protein, carb, fat)
            },
            onAddPowder = { name, grams, kcal, protein, carb, fat ->
                viewModel.addPowderPreset(name, grams, kcal, protein, carb, fat)
            },
            onDelete = { viewModel.deletePreset(it) }
        )
    }

    editingRecord?.let { record ->
        EditWaterDialog(
            record = record,
            presets = state.presets,
            onDismiss = { editingRecord = null },
            onSaveLiquid = { p, ml ->
                viewModel.updateDrink(record, p, ml)
                editingRecord = null
            },
            onSavePowder = { p, s, ml ->
                viewModel.updateMixedDrink(record, p, s, ml)
                editingRecord = null
            }
        )
    }
}

/** 水种说明：液体型看每 100ml，冲调型看每份干粉 */
private fun presetDetail(preset: DrinkPreset): String =
    if (preset.kind == DrinkKind.POWDER) {
        "冲调型 · 每份 ${preset.servingGrams.roundToInt()} g：" +
            "热量 ${preset.caloriesPerServing.roundToInt()} kcal · " +
            "蛋白质 ${f1(preset.proteinPerServing)} g · " +
            "碳水 ${f1(preset.carbPerServing)} g · 脂肪 ${f1(preset.fatPerServing)} g"
    } else {
        "液体型 · 每 100ml：水分 ${(preset.waterRatio * 100).roundToInt()}% · " +
            "热量 ${preset.caloriesPer100ml.roundToInt()} kcal · " +
            "蛋白质 ${preset.proteinPer100ml} g"
    }

@Composable
private fun LiquidIntakeSection(
    volume: Float,
    preview: DrinkNutrition?,
    preset: DrinkPreset?,
    onVolumeChange: (Float) -> Unit,
    onConfirm: () -> Unit
) {
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
        onValueChange = onVolumeChange,
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
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth(),
        enabled = preset != null && volume.roundToInt() > 0
    ) {
        Text("饮用 ${volume.roundToInt()} ml" + (preset?.let { " · ${it.name}" } ?: ""))
    }
}

@Composable
private fun PowderIntakeSection(
    preset: DrinkPreset,
    servings: Float,
    waterMl: Int,
    preview: PowderNutrition?,
    onServingsChange: (Float) -> Unit,
    onWaterChange: (Float) -> Unit,
    onConfirm: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("冲调份数", style = MaterialTheme.typography.titleLarge)
        Text(
            "${servingsLabel(servings)} 份",
            style = MaterialTheme.typography.titleMedium,
            color = DrinkBlue
        )
    }
    Slider(
        value = servings,
        onValueChange = onServingsChange,
        valueRange = 0.5f..4f,
        steps = 6
    )
    Text(
        "一勺 / 一包 = 1 份；营养总量只随份数变化",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("加水量", style = MaterialTheme.typography.titleMedium)
        Text(
            "$waterMl ml",
            style = MaterialTheme.typography.titleMedium,
            color = DrinkBlue
        )
    }
    Slider(
        value = waterMl.toFloat(),
        onValueChange = onWaterChange,
        valueRange = 50f..800f,
        steps = 74
    )
    Text(
        "水量只改变总容量与浓度，不改变蛋白质等营养总量",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (preview != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            "本次：总容量 ${preview.totalMl} ml（其中水 ${preview.waterMl} ml）· " +
                "干粉 ${f1(preview.powderGrams)} g · 热量 ${preview.caloriesKcal} kcal · " +
                "蛋白质 ${f1(preview.proteinG)} g · 碳水 ${f1(preview.carbG)} g · 脂肪 ${f1(preview.fatG)} g · " +
                "浓度 ${preview.kcalPer100ml.roundToInt()} kcal/100ml",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth(),
        enabled = waterMl > 0 && servings > 0f
    ) {
        Text("冲调 ${servingsLabel(servings)} 份 · ${preset.name} · 水 $waterMl ml")
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
                val name = record.drinkName.ifBlank { record.type.label }
                Text(
                    if (record.servings > 0f) {
                        "$name ×${servingsLabel(record.servings)} · 加水 ${record.waterMl} ml"
                    } else {
                        "$name  ${record.volumeMl} ml"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                val details = buildList {
                    add("水分 ${if (record.waterMl > 0) record.waterMl else record.volumeMl} ml")
                    if (record.servings > 0f) add("总容量 ${record.volumeMl} ml")
                    if (record.caloriesKcal > 0) add("${record.caloriesKcal} kcal")
                    if (record.proteinG > 0f) add("蛋白 ${f1(record.proteinG)} g")
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
    onSaveLiquid: (DrinkPreset, Int) -> Unit,
    onSavePowder: (DrinkPreset, Float, Int) -> Unit
) {
    val isPowder = record.servings > 0f
    var presetId by remember {
        mutableStateOf(
            presets.firstOrNull { it.name == record.drinkName && it.kind == DrinkKind.LIQUID }?.id
                ?: presets.firstOrNull { it.kind == DrinkKind.LIQUID }?.id
                ?: 0L
        )
    }
    var volume by remember { mutableStateOf(record.volumeMl.toFloat()) }
    var servings by remember { mutableStateOf(record.servings.takeIf { it > 0f } ?: 1f) }

    val liquidPreset = presets.firstOrNull { it.id == presetId && it.kind == DrinkKind.LIQUID }
    val powderPreset = remember(record, presets) {
        presets.firstOrNull { it.name == record.drinkName && it.kind == DrinkKind.POWDER }
    }
    val waterMl = volume.roundToInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPowder) "修改冲调记录" else "修改饮水记录") },
        text = {
            Column(
                Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPowder) {
                    if (powderPreset == null) {
                        Text(
                            "该冲调水种已被删除，无法修改",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "${powderPreset.name} · 每份 ${powderPreset.servingGrams.roundToInt()} g：" +
                                "蛋白 ${f1(powderPreset.proteinPerServing)} g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("份数 ${servingsLabel(servings)}", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = servings,
                            onValueChange = { servings = it },
                            valueRange = 0.5f..4f,
                            steps = 6
                        )
                        Text("加水量 $waterMl ml", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            valueRange = 50f..800f,
                            steps = 74
                        )
                        val mix = DrinkCalculator.mixOf(powderPreset, servings, waterMl)
                        Text(
                            "总容量 ${mix.totalMl} ml · 热量 ${mix.caloriesKcal} kcal · " +
                                "蛋白 ${f1(mix.proteinG)} g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.filter { it.kind == DrinkKind.LIQUID }.forEach { p ->
                            Chip(p.name, p.id == (liquidPreset?.id ?: -1L), onClick = { presetId = p.id })
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isPowder) {
                        powderPreset?.let { onSavePowder(it, servings, waterMl) } ?: onDismiss()
                    } else {
                        liquidPreset?.let { onSaveLiquid(it, volume.roundToInt()) } ?: onDismiss()
                    }
                },
                enabled = if (isPowder) {
                    powderPreset != null && servings > 0f && waterMl > 0
                } else {
                    liquidPreset != null && volume.roundToInt() > 0
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomPresetDialog(
    customPresets: List<DrinkPreset>,
    onDismiss: () -> Unit,
    onAddLiquid: (String, Int, Float, Float, Float, Float) -> Unit,
    onAddPowder: (String, Float, Float, Float, Float, Float) -> Unit,
    onDelete: (DrinkPreset) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var powder by remember { mutableStateOf(false) }
    var ratio by remember { mutableStateOf("100") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var servingGrams by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义水种") },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("液体型", !powder, onClick = { powder = false })
                    Chip("冲调型", powder, onClick = { powder = true })
                }
                Text(
                    if (powder) {
                        "按每份干粉（一勺/一包）填写营养；冲调水量只影响总容量与浓度，不改变营养总量"
                    } else {
                        "按每 100 ml 填写比例，无照片时直接按比例计算"
                    },
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
                if (powder) {
                    NumField("每份干粉重量 (g)", servingGrams, decimal = true) { servingGrams = it }
                    NumField("每份热量 (kcal/份)", kcal, decimal = true) { kcal = it }
                    NumField("每份蛋白质 (g/份)", protein, decimal = true) { protein = it }
                    NumField("每份碳水 (g/份)", carb, decimal = true) { carb = it }
                    NumField("每份脂肪 (g/份)", fat, decimal = true) { fat = it }
                } else {
                    NumField("水分比例 (%)", ratio, decimal = false) { ratio = it }
                    NumField("热量 (kcal/100ml)", kcal, decimal = true) { kcal = it }
                    NumField("蛋白质 (g/100ml)", protein, decimal = true) { protein = it }
                    NumField("碳水 (g/100ml)", carb, decimal = true) { carb = it }
                    NumField("脂肪 (g/100ml)", fat, decimal = true) { fat = it }
                }
                Button(
                    onClick = {
                        if (powder) {
                            onAddPowder(
                                name,
                                servingGrams.toFloatOrNull() ?: 0f,
                                kcal.toFloatOrNull() ?: 0f,
                                protein.toFloatOrNull() ?: 0f,
                                carb.toFloatOrNull() ?: 0f,
                                fat.toFloatOrNull() ?: 0f
                            )
                        } else {
                            onAddLiquid(
                                name,
                                ratio.toIntOrNull() ?: 100,
                                kcal.toFloatOrNull() ?: 0f,
                                protein.toFloatOrNull() ?: 0f,
                                carb.toFloatOrNull() ?: 0f,
                                fat.toFloatOrNull() ?: 0f
                            )
                        }
                        name = ""; ratio = "100"; kcal = ""; protein = ""; carb = ""
                        fat = ""; servingGrams = ""
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (powder) "添加冲调水种" else "添加水种") }

                if (customPresets.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    customPresets.forEach { p ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    if (p.kind == DrinkKind.POWDER) {
                                        "冲调 · 每份 ${p.servingGrams.roundToInt()} g · " +
                                            "${p.caloriesPerServing.roundToInt()} kcal/份"
                                    } else {
                                        "液体 · 水分 ${(p.waterRatio * 100).roundToInt()}% · " +
                                            "${p.caloriesPer100ml.roundToInt()} kcal/100ml"
                                    },
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

/** 1 份显示为「1」，1.5 份显示为「1.5」 */
private fun servingsLabel(value: Float): String =
    if (value % 1f == 0f) value.roundToInt().toString()
    else String.format(Locale.CHINA, "%.1f", value)

private fun f1(value: Float): String = String.format(Locale.CHINA, "%.1f", value)
