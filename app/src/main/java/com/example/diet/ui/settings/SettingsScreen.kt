package com.example.diet.ui.settings

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.diet.domain.model.ActivityLevel
import com.example.diet.domain.model.Gender
import com.example.diet.ui.component.Chip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val p = state.profile

    Scaffold(
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

                    NumberField("年龄", p.age.toString()) { viewModel.updateProfile { copy(age = it) } }
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
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("大模型 API 配置", style = MaterialTheme.typography.titleLarge)
                    Text("选择厂商并填写你自己的 API Key（仅保存在本机加密存储）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.vendors.forEach { v ->
                            Chip(v.name, selected = v.id == state.selectedVendorId, onClick = { viewModel.selectVendor(v.id) })
                        }
                    }
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = { viewModel.setApiKey(it) },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { viewModel.testConnection() }, enabled = !state.testing) {
                        Text(if (state.testing) "测试中…" else "连接测试")
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

            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                viewModel.clearMessage()
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
