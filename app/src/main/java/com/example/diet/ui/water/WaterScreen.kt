package com.example.diet.ui.water

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.diet.domain.NutritionCalculator
import com.example.diet.domain.model.DrinkType
import com.example.diet.ui.component.Chip
import com.example.diet.ui.navigation.Routes

@Composable
fun WaterScreen(navController: NavHostController, viewModel: WaterViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf(DrinkType.WATER) }
    var volume by remember { mutableIntStateOf(200) }
    var customVolume by remember { mutableStateOf("") }
    var milkTeaCal by remember { mutableStateOf("") }

    val waterGoal = state.profile?.waterGoalMl ?: 2000
    val progress = if (waterGoal > 0) state.volume.toFloat() / waterGoal else 0f

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("喝水", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { navController.navigate(Routes.REMINDER) }) {
                Icon(Icons.Filled.Schedule, contentDescription = "提醒设置")
            }
        }

        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("今日饮水 ${state.volume} / ${waterGoal} ml", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = Color(0xFF0288D1),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (state.calories > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("饮品额外热量：${state.calories} kcal（含奶茶）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("饮品类型", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DrinkType.values().forEach { type ->
                Chip(text = type.label, selected = type == selectedType, onClick = { selectedType = type })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("容量", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(200, 300, 500).forEach { v ->
                TextButton(onClick = { volume = v; customVolume = "" }) {
                    Text("$v ml")
                }
            }
        }
        OutlinedTextField(
            value = customVolume,
            onValueChange = { customVolume = it.filter { c -> c.isDigit() } },
            label = { Text("自定义容量 (ml)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        if (selectedType == DrinkType.MILK_TEA) {
            Spacer(Modifier.height(8.dp))
            val auto = NutritionCalculator.estimateDrink(selectedType, (customVolume.toIntOrNull() ?: volume)).first
            Text("预计热量约 $auto kcal（同时计入水分）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = milkTeaCal,
                onValueChange = { milkTeaCal = it.filter { c -> c.isDigit() } },
                label = { Text("手动修正热量 (可选)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.FilledIconButton(onClick = {
            val finalVolume = customVolume.toIntOrNull() ?: volume
            val explicit = if (selectedType == DrinkType.MILK_TEA && milkTeaCal.isNotBlank())
                milkTeaCal.toIntOrNull() else null
            viewModel.addDrink(selectedType, finalVolume, explicit)
        }) {
            Icon(Icons.Filled.Add, contentDescription = "添加")
        }

        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.records, key = { it.id }) { rec ->
                WaterItemRow(rec = rec, onDelete = { viewModel.delete(rec) })
            }
        }
    }
}

@Composable
private fun WaterItemRow(rec: com.example.diet.data.local.entity.WaterRecord, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${rec.type.label}  ${rec.volumeMl} ml", style = MaterialTheme.typography.titleMedium)
                val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(rec.recordedAt))
                Text(
                    "$time" + if (rec.caloriesKcal > 0) " · ${rec.caloriesKcal} kcal" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color(0xFFE53935))
            }
        }
    }
}
