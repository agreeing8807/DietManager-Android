package com.example.diet.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.diet.ui.component.ProgressRing
import com.example.diet.ui.component.RingLabel
import com.example.diet.ui.component.SectionCard
import com.example.diet.ui.navigation.Routes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController, viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val goal = state.profile?.dailyCalorieGoal ?: 1800
    val waterGoal = state.profile?.waterGoalMl ?: 2000
    val remaining = (goal - state.calorieIntake).coerceAtLeast(0)
    val calorieProgress = if (goal > 0) state.calorieIntake.toFloat() / goal else 0f
    val waterProgress = if (waterGoal > 0) state.waterVolume.toFloat() / waterGoal else 0f

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("今日概览", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    state.profile?.let { "目标体重 ${it.targetWeightKg.toInt()}kg" } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SectionCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ProgressRing(
                            progress = calorieProgress,
                            colors = listOf(Color(0xFFFFB74D), Color(0xFFFF7043))
                        ) {
                            RingLabel(
                                primary = "${state.calorieIntake}",
                                secondary = "已摄入 / ${goal} kcal"
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "剩余可用 ${remaining} kcal",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (remaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                var waterAmount by remember { mutableStateOf(250f) }
                SectionCard {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.LocalDrink,
                                    contentDescription = null,
                                    Modifier.size(22.dp),
                                    tint = Color(0xFF0288D1)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("饮水", style = MaterialTheme.typography.titleLarge)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${state.waterVolume} / ${waterGoal} ml",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(onClick = { navController.navigate(Routes.REMINDER) }) {
                                    Icon(
                                        Icons.Filled.Notifications,
                                        contentDescription = "饮水提醒",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                Modifier.fillMaxWidth(waterProgress.coerceIn(0f, 1f))
                                    .height(14.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))
                                        )
                                    )
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "本次饮水量",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${waterAmount.roundToInt()} ml",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = waterAmount,
                            onValueChange = { waterAmount = it },
                            valueRange = 0f..500f,
                            steps = 9
                        )
                        Button(
                            onClick = {
                                val ml = waterAmount.roundToInt()
                                if (ml > 0) scope.launch { viewModel.quickAddWater(ml) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = waterAmount.roundToInt() > 0
                        ) {
                            Icon(Icons.Filled.LocalDrink, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("饮水 ${waterAmount.roundToInt()} ml")
                        }
                    }
                }
            }

            item {
                QuickEntryCard(
                    title = "记录三餐",
                    subtitle = "拍照或文字，AI 分析营养",
                    icon = Icons.Filled.Restaurant
                ) { navController.navigate("meal") }
            }
        }
    }
}

@Composable
private fun QuickEntryCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
