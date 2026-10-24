package com.example.diet.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiListScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已添加的 API") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.entries.isEmpty()) {
                Text("还没有添加任何 API", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("厂商", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Text("模型", Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium)
                    Text("API Key", Modifier.weight(1.4f), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(48.dp))
                }
                HorizontalDivider()
                state.entries.forEach { entry ->
                    val vendorName = state.vendors.firstOrNull { it.id == entry.vendorId }?.name
                        ?: entry.vendorId
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(vendorName, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text(entry.model, Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall)
                        Text(maskApiKey(entry.apiKey), Modifier.weight(1.4f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { viewModel.deleteApi(entry) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color(0xFFE53935))
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/** 中间用 * 掩码，界面上无法还原查看 */
private fun maskApiKey(key: String): String =
    if (key.length <= 8) "********" else key.take(4) + "****" + key.takeLast(4)
