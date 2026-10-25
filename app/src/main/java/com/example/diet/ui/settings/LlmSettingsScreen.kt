package com.example.diet.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.diet.ui.component.Chip
import com.example.diet.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmSettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("大模型设置") },
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
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("API 配置", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "选择厂商/模型并填写 API Key，点“添加”会先校验连接再保存",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("提示语（Prompt）", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "可自定义发送给模型的系统提示语；留空则使用默认。修改后自动保存。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = state.promptIdentify,
                        onValueChange = { viewModel.setPromptIdentify(it) },
                        label = { Text("食物识别 Prompt") },
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
        }
    }
}
