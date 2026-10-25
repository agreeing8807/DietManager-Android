package com.example.diet.ui.meal

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diet.data.local.entity.MealItem
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.domain.MealItems
import com.example.diet.domain.model.DataStatus
import com.example.diet.domain.model.MealType
import com.example.diet.ui.component.Chip
import com.example.diet.ui.component.SectionCard
import com.example.diet.util.DateUtils
import java.io.ByteArrayOutputStream
import java.time.LocalTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MealScreen(viewModel: MealViewModel = hiltViewModel()) {
    val day by viewModel.dayState.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detailItems by viewModel.detailItems.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf(MealType.fromHour(LocalTime.now().hour)) }
    var description by remember { mutableStateOf("") }
    var captured by remember { mutableStateOf<ByteArray?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var showSuggestions by remember { mutableStateOf(false) }
    var detailMeal by remember { mutableStateOf<MealRecord?>(null) }
    var editingMeal by remember { mutableStateOf<MealRecord?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            captured = stream.toByteArray()
            preview = it.asImageBitmap()
        }
    }

    val launchCamera = {
        try {
            cameraLauncher.launch(null)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "无法打开相机：${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            android.widget.Toast.makeText(context, "未授予相机权限，无法拍照", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val goal = day.profile.dailyCalorieGoal.coerceAtLeast(1)
    val remaining = (goal - day.totalCalories).coerceAtLeast(0)

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("吃饭", style = MaterialTheme.typography.headlineMedium)
        }

        // ---------------- 日期切换 ----------------
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.shiftDate(-1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "前一天")
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        DateUtils.dayLabel(day.date),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (day.isToday) "今天" else "往日记录",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { viewModel.shiftDate(1) },
                    enabled = day.today.isNotBlank() && day.date < day.today
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "后一天")
                }
            }
            if (!day.isToday) {
                TextButton(onClick = { viewModel.backToToday() }, modifier = Modifier.fillMaxWidth()) {
                    Text("回到今天")
                }
            }
        }

        // ---------------- 添加（仅当天可写） ----------------
        item {
            if (!day.isToday) {
                SectionCard {
                    Text("往日记录仅可查看、修改或删除", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "新增用餐只能记录到今天：请点上方「回到今天」或日期右侧箭头切回今天再添加。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("记录一餐", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            showSuggestions = true
                            viewModel.loadSuggestions()
                        }
                    ) {
                        Icon(Icons.Filled.Restaurant, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("吃什么")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "餐次已按当前时间自动选择，可手动更改",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MealType.values().forEach { type ->
                        Chip(
                            text = type.label,
                            selected = type == selectedType,
                            onClick = { selectedType = type }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述这餐吃了什么") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text(if (preview != null) "已拍摄（重拍）" else "拍照分析")
                    }
                    preview?.let {
                        Image(it, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                    }
                    Spacer(Modifier.weight(1f))
                    FilledIconButton(
                        onClick = {
                            // 只有 VM 受理后才清空输入，避免重复点击/被拒时丢失内容
                            val accepted = viewModel.addMeal(selectedType, description, captured)
                            if (accepted) {
                                description = ""
                                captured = null
                                preview = null
                            }
                        },
                        enabled = day.isToday && !state.analyzing &&
                            (description.isNotBlank() || captured != null)
                    ) {
                        if (state.analyzing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Add, contentDescription = "添加")
                    }
                }
                if (preview != null && description.isBlank()) {
                    Text(
                        "只拍照也可以：会用支持视觉的模型按图片估算营养",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.message?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
            }
        }

        // ---------------- 当日汇总 ----------------
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "合计 ${day.totalCalories} kcal（含饮品）",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("目标 $goal kcal · 剩余 $remaining kcal", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "蛋白质 ${day.proteinG.roundToInt()} g · 碳水 ${day.carbG.roundToInt()} g · 脂肪 ${day.fatG.roundToInt()} g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (day.pendingMeals > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${day.pendingMeals} 条记录营养数据不完整，合计可能偏低",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "点按任意一餐可查看并修正食物明细",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---------------- 时间线 ----------------
        item {
            Text("${if (day.isToday) "今天" else DateUtils.dayLabel(day.date)}吃了什么", style = MaterialTheme.typography.titleMedium)
        }
        if (day.meals.isEmpty()) {
            item {
                Text(
                    "这一天还没有用餐记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(day.meals, key = { it.id }) { meal ->
                MealTimelineRow(
                    meal = meal,
                    onDetail = {
                        detailMeal = meal
                        viewModel.openDetail(meal)
                    },
                    onEdit = { editingMeal = meal },
                    onDelete = { viewModel.deleteMeal(meal) }
                )
            }
        }
    }

    if (showSuggestions) {
        SuggestionDialog(
            suggestions = state.suggestions,
            loading = state.suggesting,
            error = state.error,
            remaining = remaining,
            onRefresh = { viewModel.loadSuggestions() },
            onDismiss = {
                showSuggestions = false
                viewModel.clearError()
            }
        )
    }

    detailMeal?.let { meal ->
        MealItemDialog(
            meal = meal,
            items = detailItems,
            analyzing = state.analyzing,
            onUpdateGrams = { item, grams -> viewModel.updateItemGrams(item, grams) },
            onDelete = { item -> viewModel.deleteItem(item) },
            onReanalyze = { viewModel.reanalyze(meal) },
            onDismiss = {
                detailMeal = null
                viewModel.closeDetail()
            }
        )
    }

    editingMeal?.let { meal ->
        EditMealDialog(
            meal = meal,
            onDismiss = { editingMeal = null },
            onSave = { type, desc, calories ->
                viewModel.updateMeal(meal, type, desc, calories)
                editingMeal = null
            }
        )
    }
}

@Composable
private fun MealTimelineRow(
    meal: MealRecord,
    onDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onDetail),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp)) {
            Column(Modifier.width(56.dp)) {
                Text(
                    DateUtils.formatTime(meal.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    meal.type.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(Modifier.weight(1f)) {
                Text(meal.description, style = MaterialTheme.typography.titleMedium)
                // 状态来自数据字段，不再依赖 summary 文案前缀
                val statusText = when (meal.status) {
                    DataStatus.MISSING ->
                        if (meal.analyzedByAi) "营养数据缺失 · AI 未查到可靠来源" else "营养数据缺失"
                    DataStatus.PARTIAL -> "${meal.caloriesKcal} kcal · 部分食物缺少数据"
                    DataStatus.ESTIMATED -> "${meal.caloriesKcal} kcal · AI 分析"
                    DataStatus.CONFIRMED -> "${meal.caloriesKcal} kcal · 已确认"
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (meal.status.needsAttention) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                if (meal.summary.isNotBlank()) {
                    Text(
                        meal.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "蛋白质 ${meal.proteinG.roundToInt()} g · 碳水 ${meal.carbG.roundToInt()} g · 脂肪 ${meal.fatG.roundToInt()} g",
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

/**
 * 食物明细：每项只改克数或删除，营养按每 100g 基准本地重算。
 * 这是"数据可修正"的入口 —— 识别错了不用重新分析整餐。
 */
@Composable
private fun MealItemDialog(
    meal: MealRecord,
    items: List<MealItem>,
    analyzing: Boolean,
    onUpdateGrams: (MealItem, Float) -> Unit,
    onDelete: (MealItem) -> Unit,
    onReanalyze: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("食物明细") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(meal.description, style = MaterialTheme.typography.titleMedium)
                if (items.isEmpty()) {
                    Text(
                        "这条记录没有条目明细（多为历史数据或手动记录），合计仍按记录本身计算",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    items.forEach { item ->
                        MealItemRow(
                            item = item,
                            onUpdateGrams = { grams -> onUpdateGrams(item, grams) },
                            onDelete = { onDelete(item) }
                        )
                    }
                    HorizontalDivider()
                    Text(
                        "改克数后按每 100g 基准本地重算，整餐合计与状态会自动更新；" +
                            "摘要文字仍为分析时的快照",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 条件由当前条目推导，补全成功后按钮会自动消失
                if (items.isEmpty() || items.any { !it.hasBasis }) {
                    TextButton(onClick = onReanalyze, enabled = !analyzing) {
                        Text(if (items.isEmpty()) "重新识别这餐" else "补全缺失营养")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}

/** 1 份显示为「1」，1.5 份显示为「1.5」 */
private fun servingsLabel(value: Float): String =
    if (value % 1f == 0f) value.roundToInt().toString()
    else String.format(java.util.Locale.CHINA, "%.1f", value)

@Composable
private fun MealItemRow(item: MealItem, onUpdateGrams: (Float) -> Unit, onDelete: () -> Unit) {
    var gramsText by remember(item.id, item.grams) { mutableStateOf(item.grams.roundToInt().toString()) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                buildString {
                    if (item.brand.isNotBlank()) append("${item.brand} ")
                    append(item.name)
                    if (item.servings > 0f) append(" ×${servingsLabel(item.servings)} 份")
                },
                style = MaterialTheme.typography.bodyLarge
            )
            val nutrition = MealItems.nutritionOf(item)
            Text(
                buildString {
                    if (item.hasBasis) {
                        append("约 ${nutrition.kcal} kcal · 蛋白 ${nutrition.proteinG.roundToInt()} g · ")
                    } else {
                        append("缺营养数据 · ")
                    }
                    append(item.source.ifBlank { "无来源" })
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (item.hasBasis) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        OutlinedTextField(
            value = gramsText,
            onValueChange = { text -> gramsText = text.filter { it.isDigit() || it == '.' } },
            label = { Text("g") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(76.dp)
        )
        IconButton(onClick = { gramsText.toFloatOrNull()?.let(onUpdateGrams) }) {
            Icon(Icons.Filled.Check, contentDescription = "应用克数", Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除", Modifier.size(18.dp), tint = Color(0xFFE53935))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditMealDialog(
    meal: MealRecord,
    onDismiss: () -> Unit,
    onSave: (MealType, String, Int) -> Unit
) {
    var type by remember { mutableStateOf(meal.type) }
    var desc by remember { mutableStateOf(meal.description) }
    var calories by remember { mutableStateOf(meal.caloriesKcal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改用餐记录") },
        text = {
            Column(
                Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("餐次", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MealType.values().forEach { t ->
                        Chip(t.label, t == type, onClick = { type = t })
                    }
                }
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { v -> calories = v.filter { it.isDigit() } },
                    label = { Text("热量 (kcal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "若该餐已有食物明细，营养数值以明细为准，此处修改不会生效；请点开记录修改克数。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(type, desc, calories.toIntOrNull() ?: meal.caloriesKcal) }) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun SuggestionDialog(
    suggestions: List<com.example.diet.domain.model.FoodSuggestion>,
    loading: Boolean,
    error: String?,
    remaining: Int,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("吃什么建议") },
        text = {
            Column(
                Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "剩余 $remaining kcal，按此推荐更合适的食物",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when {
                    loading -> Text("正在生成建议…")
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                    suggestions.isEmpty() -> Text("暂无建议，点“换一批”重试")
                    else -> suggestions.forEach { s ->
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${s.caloriesKcal} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                s.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh, enabled = !loading) { Text("换一批") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
