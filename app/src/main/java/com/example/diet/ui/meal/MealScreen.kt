package com.example.diet.ui.meal

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diet.domain.model.MealType
import com.example.diet.ui.component.Chip
import com.example.diet.ui.component.MacroBar
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(viewModel: MealViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf(MealType.BREAKFAST) }
    var description by remember { mutableStateOf("") }
    var captured by remember { mutableStateOf<ByteArray?>(null) }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            captured = stream.toByteArray()
            preview = it.asImageBitmap()
            // 不能 recycle：preview 仍持有该 Bitmap 用于显示，回收会导致 Canvas 绘制已回收 Bitmap 而崩溃
        }
    }

    val launchCamera = {
        try {
            cameraLauncher.launch(null)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "无法打开相机：${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 拍照前主动申请 CAMERA 权限：声明了 CAMERA 却未授权时，ACTION_IMAGE_CAPTURE 会抛 SecurityException
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            android.widget.Toast.makeText(context, "未授予相机权限，无法拍照", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("三餐记录", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MealType.values().forEach { type ->
                Chip(
                    text = type.label,
                    selected = type == selectedType,
                    onClick = { selectedType = type; viewModel.setType(type) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

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
            androidx.compose.material3.FilledIconButton(onClick = {
                viewModel.addMeal(selectedType, description, captured)
                description = ""
                captured = null
                preview = null
            }, enabled = !state.analyzing) {
                if (state.analyzing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Add, contentDescription = "添加")
            }
        }

        state.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            viewModel.clearMessage()
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(meals, key = { it.id }) { meal ->
                MealItemCard(meal = meal, onDelete = { viewModel.deleteMeal(meal) })
            }
        }
    }
}

@Composable
private fun MealItemCard(meal: com.example.diet.data.local.entity.MealRecord, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(meal.description, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${meal.caloriesKcal} kcal" + if (meal.analyzedByAi) " · AI 分析" else " · 手动",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color(0xFFE53935))
                }
            }
            if (meal.summary.isNotBlank()) {
                Text(meal.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            MacroBar("蛋白质", meal.proteinG, (meal.proteinG + 30f).coerceAtLeast(30f), Color(0xFF42A5F5))
            MacroBar("碳水", meal.carbG, (meal.carbG + 60f).coerceAtLeast(60f), Color(0xFFFFB74D))
            MacroBar("脂肪", meal.fatG, (meal.fatG + 25f).coerceAtLeast(25f), Color(0xFFAB47BC))
        }
    }
}
