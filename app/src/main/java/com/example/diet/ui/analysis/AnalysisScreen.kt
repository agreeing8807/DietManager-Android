package com.example.diet.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diet.ui.component.SectionCard

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val suggestionLoading by viewModel.suggestionLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val goal = state.profile?.dailyCalorieGoal ?: 1800
    val remaining = (goal - state.calorieIntake).coerceAtLeast(0)
    val over = state.calorieIntake > goal
    val statusColor = if (over) Color(0xFFE53935) else Color(0xFF43A047)
    val statusText = if (over) "已超出 ${state.calorieIntake - goal} kcal" else "剩余 ${remaining} kcal"

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("分析与建议", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            SectionCard {
                Text("今日热量对比", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("摄入 ${state.calorieIntake} kcal", style = MaterialTheme.typography.bodyLarge)
                    Text("目标 $goal kcal", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(6.dp))
                Text(statusText, color = statusColor, style = MaterialTheme.typography.titleMedium)
            }
        }

        item {
            SectionCard {
                Text("AI 餐食分析", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (state.meals.isEmpty()) {
                    Text("今天还没有记录三餐", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.meals.forEach { meal ->
                        if (meal.summary.isNotBlank()) {
                            Text("· ${meal.type.label}：${meal.summary}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("推荐食物", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = { viewModel.loadSuggestions() }, enabled = !suggestionLoading) {
                    if (suggestionLoading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("获取建议")
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                viewModel.clearError()
            }
        }

        items(suggestions, key = { it.name }) { s ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium)
                        Text("${s.caloriesKcal} kcal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(s.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
