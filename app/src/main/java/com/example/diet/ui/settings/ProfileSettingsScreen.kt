package com.example.diet.ui.settings

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
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
import com.example.diet.domain.model.Gender
import com.example.diet.ui.component.Chip
import com.example.diet.util.AgeCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
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
                title = { Text("个人信息") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("基础资料", style = MaterialTheme.typography.titleLarge)

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
            NumberField("身高 (cm)", p.heightCm.toInt().toString()) {
                viewModel.updateProfile { copy(heightCm = it.toFloat()) }
            }
            NumberField("当前体重 (kg)", p.currentWeightKg.toInt().toString()) {
                viewModel.updateProfile { copy(currentWeightKg = it.toFloat()) }
            }
            NumberField("目标体重 (kg)", p.targetWeightKg.toInt().toString()) {
                viewModel.updateProfile { copy(targetWeightKg = it.toFloat()) }
            }
            NumberField("每日饮水目标 (ml)", p.waterGoalMl.toString()) {
                viewModel.updateProfile { copy(waterGoalMl = it) }
            }
            NumberField(
                label = "每日开始时间（0-12 点，默认 4）",
                value = p.dayStartHour.toString()
            ) { viewModel.updateProfile { copy(dayStartHour = it.coerceIn(0, 12)) } }
            Text(
                "在设定的时间之前记录的饮食/饮水会算作前一天，适合夜班或晚睡作息。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("活动水平", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.diet.domain.model.ActivityLevel.values().take(3).forEach { level ->
                    Chip(
                        level.label.substringBefore("（"),
                        selected = p.activity == level,
                        onClick = { viewModel.updateProfile { copy(activity = level) } }
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.diet.domain.model.ActivityLevel.values().drop(3).forEach { level ->
                    Chip(
                        level.label.substringBefore("（"),
                        selected = p.activity == level,
                        onClick = { viewModel.updateProfile { copy(activity = level) } }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(onClick = { viewModel.saveProfile() }, modifier = Modifier.fillMaxWidth()) {
                Text("保存个人资料")
            }
            OutlinedButton(onClick = { viewModel.recalcCalorieGoal() }, modifier = Modifier.fillMaxWidth()) {
                Text("按当前体重重新计算热量目标")
            }
            Text(
                "当前热量目标：${p.dailyCalorieGoal} kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
