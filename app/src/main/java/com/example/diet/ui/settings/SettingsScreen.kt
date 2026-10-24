package com.example.diet.ui.settings

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.diet.domain.model.ActivityLevel
import com.example.diet.domain.model.Gender
import com.example.diet.ui.component.Chip
import com.example.diet.ui.navigation.Routes
import com.example.diet.util.AgeCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val p = state.profile
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("个人资料与目标", style = MaterialTheme.typography.titleLarge)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("男", selected = p.gender == Gender.MALE, onClick = { viewModel.updateProfile { copy(gender = Gender.MALE) } })
                        Chip("女", selected = p.gender == Gender.FEMALE, onClick = { viewModel.updateProfile { copy(gender = Gender.FEMALE) } })
                    }

                    BirthdayField(
                        birthday = p.birthday,
                        age = p.age,
                        onChange = { millis ->
                            viewModel.updateProfile {
                                copy(birthday = millis, age = AgeCalculator.ageFromBirthday(millis))
                            }
                        }
                    )
                    NumberField("身高 (cm)", p.heightCm.toInt().toString()) { viewModel.updateProfile { copy(heightCm = it.toFloat()) } }
                    NumberField("当前体重 (kg)", p.currentWeightKg.toInt().toString()) { viewModel.updateProfile { copy(currentWeightKg = it.toFloat()) } }
                    NumberField("目标体重 (kg)", p.targetWeightKg.toInt().toString()) { viewModel.updateProfile { copy(targetWeightKg = it.toFloat()) } }
                    NumberField("每日饮水目标 (ml)", p.waterGoalMl.toString()) { viewModel.updateProfile { copy(waterGoalMl = it) } }

                    // 活动量选择
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = p.activity.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("活动水平") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "选择")
                                }
                            }
                        )
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            ActivityLevel.values().forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level.label) },
                                    onClick = {
                                        viewModel.updateProfile { copy(activity = level) }
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(onClick = { viewModel.saveProfile() }, modifier = Modifier.fillMaxWidth()) {
                        Text("保存资料并计算目标热量")
                    }
                    TextButton(
                        onClick = { navController.navigate(Routes.ONBOARDING) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("重新查看新手引导")
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("大模型 API 配置", style = MaterialTheme.typography.titleLarge)
                    Text("选择厂商/模型并填写 API Key，点“添加”会先校验连接再保存", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.vendors.forEach { v ->
                            Chip(v.name, selected = v.id == state.newVendorId, onClick = { viewModel.selectNewVendor(v.id) })
                        }
                    }
                    OutlinedTextField(
                        value = state.newModel,
                        onValueChange = { viewModel.setNewModel(it) },
                        label = { Text("模型") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.newApiKey,
                        onValueChange = { viewModel.setNewApiKey(it) },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.addApi() }, enabled = !state.adding, modifier = Modifier.weight(1f)) {
                            Text(if (state.adding) "添加中…" else "添加")
                        }
                        OutlinedButton(onClick = { navController.navigate(Routes.API_LIST) }, modifier = Modifier.weight(1f)) {
                            Text("查看 API")
                        }
                    }
                    Button(onClick = { viewModel.testAll() }, enabled = !state.testing, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.testing) "测试中…" else "连接测试（全部）")
                    }
                    state.testResults.forEach { r ->
                        Text(
                            "${if (r.success) "✅" else "❌"} ${r.label}：${r.detail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (r.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("大模型提示语（Prompt）", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "可自定义发送给模型的系统提示语；留空则使用默认。修改后自动保存。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = state.promptAnalyze,
                        onValueChange = { viewModel.setPromptAnalyze(it) },
                        label = { Text("餐食分析 Prompt") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                    OutlinedTextField(
                        value = state.promptSuggest,
                        onValueChange = { viewModel.setPromptSuggest(it) },
                        label = { Text("食物推荐 Prompt") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Button(onClick = { viewModel.resetPrompts() }, modifier = Modifier.fillMaxWidth()) {
                        Text("恢复默认提示语")
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("数据管理", style = MaterialTheme.typography.titleLarge)
                    Text("导出/导入本地备份（JSON，保存在应用私有目录）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.exportData() }, modifier = Modifier.weight(1f)) { Text("导出备份") }
                        Button(onClick = { viewModel.importData() }, modifier = Modifier.weight(1f)) { Text("导入备份") }
                    }
                }
            }

        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter { c -> c.isDigit() }
            text.toIntOrNull()?.let { v -> onValueChange(v) }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BirthdayField(birthday: Long?, age: Int, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    OutlinedTextField(
        value = birthday?.let { ms -> formatter.format(Date(ms)) } ?: "未设置",
        onValueChange = {},
        readOnly = true,
        label = { Text("生日（年龄自动计算：$age 岁）") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = {
                val initCal = birthday?.let { ms -> Calendar.getInstance().apply { timeInMillis = ms } }
                    ?: Calendar.getInstance()
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val picked = Calendar.getInstance().apply {
                            clear()
                            set(year, month, day, 0, 0, 0)
                        }
                        onChange(picked.timeInMillis)
                    },
                    initCal.get(Calendar.YEAR),
                    initCal.get(Calendar.MONTH),
                    initCal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Icon(Icons.Filled.Edit, contentDescription = "选择生日")
            }
        }
    )
}
