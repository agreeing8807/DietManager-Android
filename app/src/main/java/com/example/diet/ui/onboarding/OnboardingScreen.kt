package com.example.diet.ui.onboarding

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavGraph.Companion.findStartDestination
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

@Composable
fun OnboardingScreen(
    navController: NavHostController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val p = profile

    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("欢迎使用饮食管理", style = MaterialTheme.typography.headlineMedium)
        Text(
            "先花一分钟完善资料，帮你自动计算每日热量目标",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("性别", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("男", selected = p.gender == Gender.MALE, onClick = { viewModel.update { copy(gender = Gender.MALE) } })
            Chip("女", selected = p.gender == Gender.FEMALE, onClick = { viewModel.update { copy(gender = Gender.FEMALE) } })
        }

        BirthdayField(
            birthday = p.birthday,
            age = p.age,
            onChange = { millis ->
                viewModel.update { copy(birthday = millis, age = AgeCalculator.ageFromBirthday(millis)) }
            }
        )

        NumberField("身高 (cm)", p.heightCm.toInt().toString()) { viewModel.update { copy(heightCm = it.toFloat()) } }
        NumberField("当前体重 (kg)", p.currentWeightKg.toInt().toString()) { viewModel.update { copy(currentWeightKg = it.toFloat()) } }
        NumberField("目标体重 (kg)", p.targetWeightKg.toInt().toString()) { viewModel.update { copy(targetWeightKg = it.toFloat()) } }
        NumberField("每日饮水目标 (ml)", p.waterGoalMl.toString()) { viewModel.update { copy(waterGoalMl = it) } }

        Text("活动水平", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActivityLevel.values().forEach { level ->
                Chip(
                    level.label,
                    selected = p.activity == level,
                    onClick = { viewModel.update { copy(activity = level) } }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.complete {
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("onboarded", true).apply()
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始使用")
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
