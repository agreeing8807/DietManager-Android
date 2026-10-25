package com.example.diet.ui.meal

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.domain.model.MealType
import com.example.diet.ui.component.Chip
import com.example.diet.ui.component.SectionCard
import com.example.diet.util.DateUtils
import java.io.ByteArrayOutputStream
import java.time.LocalTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(viewModel: MealViewModel = hiltViewModel()) {
    val day by viewModel.dayState.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf(MealType.fromHour(LocalTime.now().hour)) }
    var description by remember { mutableStateOf("") }
    var captured by remember { mutableStateOf<ByteArray?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var showSuggestions by remember { mutableStateOf(false) }
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

        // ---------------- 添加 ----------------
        item {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            viewModel.addMeal(selectedType, description, captured)
                            description = ""
                            captured = null
                            preview = null
                        },
                        enabled = !state.analyzing
                    ) {
                        if (state.analyzing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Add, contentDescription = "添加")
                    }
                }
                state.message?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
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
                        Text("合计 ${day.totalCalories} kcal", style = MaterialTheme.typography.titleMedium)
                        Text("目标 $goal kcal · 剩余 $remaining kcal", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "蛋白质 ${day.proteinG.roundToInt()} g · 碳水 ${day.carbG.roundToInt()} g · 脂肪 ${day.fatG.roundToInt()} g",
                        style = MaterialTheme.typography.bodySmall,
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
private fun MealTimelineRow(meal: MealRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
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
                Text(
                    "${meal.caloriesKcal} kcal" + if (meal.analyzedByAi) " · AI 分析" else " · 手动",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
